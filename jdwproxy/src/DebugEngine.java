package jdwproxy;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.Objects;

// TODO:
// prefetch and cache from proxy side
// Super Response and Super Notification from stub side
// command/reply ID management

public class DebugEngine {
	public enum Mode {
		RELAY,
		PROXY,
		STUB
	}
	public static final String JDWP_HANDSHAKE_MESSAGE = "JDWP-Handshake";

	public static final int JDWP_HANDSHAKE_FAILED = -1;
	public static final int JDWP_DEBUGGEE_CONNECTION_RESET = -2;
	public static final int JDWP_DEBUGGER_CONNECTION_RESET= -3;
	public static final int JDWP_UNKNOWN_ERROR = -0x7fffffff;
	public static final int JDWP_SUCCEEDED = 0;
	public static final int JDWP_EMPTY = 1;
	public static final int JDWP_DEBUGGEE_ACTIVE_CLOSE = 2;
	public static final int JDWP_DEBUGGER_ACTIVE_CLOSE = 3;

	private LinkedBlockingQueue<IrpBase> m_irp_queue = new LinkedBlockingQueue<>();
	private HashMap<Integer, IrpCommandToDebuggee> m_dispatchedCommands = new HashMap<>();
	private HashMap<JvmId, IrpCommandToDebuggee> m_cache = new HashMap<>();
	// private HashMap<JvmId, ThreadInfo> m_threadInfoCache = new HashMap<>();
	private int m_commandId = Integer.MIN_VALUE;
	private final Mode m_mode;
	private Serializer m_serializer = null;
	private int m_status = JDWP_SUCCEEDED;
	private final Endpoint m_incomingEndpoint;
	private final Endpoint m_outgoingEndpoint;

	public DebugEngine(Endpoint incoming, Endpoint outgoing, Mode mode) {
		m_incomingEndpoint = incoming;
		m_outgoingEndpoint = outgoing;
		m_mode = mode;
	}

	public void loop() throws InterruptedException {
		if (activeHandshake() != JDWP_SUCCEEDED || passiveHandshake() != JDWP_SUCCEEDED) {
			System.err.printf("handshake failed, quit...\n");
			return;
		}
		try {
			prefetch(JDWP.Command.VIRTUAL_MACHINE_VERSION, null);
			prefetch(JDWP.Command.VIRTUAL_MACHINE_ID_SIZES, null);
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
		Thread debuggerPacketProcessorThread = new Thread(new DebuggerPacketProcessor());
		Thread debuggeePacketProcessorThread = new Thread(new DebuggeePacketProcessor());
		debuggerPacketProcessorThread.start();
		debuggeePacketProcessorThread.start();
		while (m_status == JDWP_SUCCEEDED) {
			try {
				m_irp_queue.take().prepare();
			} catch (Exception ex) {
				m_status = JDWP_UNKNOWN_ERROR;
				ex.printStackTrace();
			}
		}
		try { m_incomingEndpoint.close(); } catch (IOException unused) { /* keep silent */ }
		try { m_outgoingEndpoint.close(); } catch (IOException unused) { /* keep silent */ }
		debuggerPacketProcessorThread.join();
		debuggeePacketProcessorThread.join();
	}

	private void prefetch(short code, byte[] data) throws IOException {
		Packet command = Packet.createCommand(Integer.MIN_VALUE, code, data);
		IrpCommandToDebuggee irp = dispatchCommand(command);
		m_cache.put(new JvmId(code), irp);
	}

	public String toString() {
		return String.format("%s || %s", m_incomingEndpoint, m_outgoingEndpoint);
	}

	int activeHandshake() {
		try {
			System.out.printf("sending handshake request\n");
			m_outgoingEndpoint.writeString(JDWP_HANDSHAKE_MESSAGE);
			System.out.printf("sent handshake request\n");

			if (!Objects.equals(m_outgoingEndpoint.readString(14), JDWP_HANDSHAKE_MESSAGE)) {
				throw new ProtocolException("Invalid JDWP handshake response from the debuggee!");
			}
			System.out.printf("received handshake response\n");
		} catch (IOException ex) {
			ex.printStackTrace();
			return JDWP_HANDSHAKE_FAILED;
		}
		return JDWP_SUCCEEDED;
	}

	int passiveHandshake() {
		try {
			if (!Objects.equals(m_incomingEndpoint.readString(14), JDWP_HANDSHAKE_MESSAGE)) {
				throw new ProtocolException("Invalid JDWP handshake request from the debugger!");
			}
			System.out.printf("received handshake request\n");

			System.out.printf("sending handshake response\n");
			m_incomingEndpoint.writeString(JDWP_HANDSHAKE_MESSAGE);
			System.out.printf("sent handshake response\n");
		} catch (IOException ex) {
			ex.printStackTrace();
			return JDWP_HANDSHAKE_FAILED;
		}
		return JDWP_SUCCEEDED;
	}

	IrpCommandToDebuggee dispatchCommand(Packet command) throws IOException {
		m_commandId = m_commandId < 0 ? 0 : m_commandId + 1;
		if (command.id > m_commandId) {
			m_commandId = command.id;
		}
		Packet packet = Packet.createCommand(m_commandId, command.code, command.data);
		IrpCommandToDebuggee irp = new IrpCommandToDebuggee(packet);
		packet.writeTo(m_outgoingEndpoint);
		m_dispatchedCommands.put(m_commandId, irp);
		return irp;
	}

	class DebuggerPacketProcessor implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					Packet packet = new Packet(m_incomingEndpoint);
					System.out.printf("Debugger->Debuggee %s\n", packet);
					assert packet.flags == Packet.COMMAND; // debugger should only send COMMAND instead of REPLY
					m_irp_queue.put(new IrpCommandFromDebugger(packet));
				}
			} catch (Exception ex) {
				try { m_irp_queue.put(new IrpException(ex)); } catch (InterruptedException unused) { /* keep silent */ }
			}
		}
	}

	class DebuggeePacketProcessor implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					Packet packet = new Packet(m_outgoingEndpoint);
					System.out.printf("Debugger<-Debuggee %s\n", packet);
					assert packet.flags == Packet.COMMAND || packet.flags == Packet.REPLY; // JDWP only allows COMMAND and REPLY
					m_irp_queue.put(packet.flags == Packet.REPLY ? new IrpReplyFromDebuggee(packet) : new IrpCommandFromDebuggee(packet));
				}
			} catch (Exception ex) {
				try { m_irp_queue.put(new IrpException(ex)); } catch (InterruptedException unused) { /* keep silent */ }
			}
		}
	}

	abstract class IrpBase extends IRP<Packet> {
		public IrpBase(Packet packet) {
			super(packet);
		}

		void prepare() throws Exception {
			boolean isAssertOn = false;
			assert isAssertOn = true;
			if (isAssertOn) {
				System.out.printf("PREPARE %s %s\n", this.getClass().getSimpleName(), this);
			}
		}

		@Override
		public void onready() throws Exception {
			boolean isAssertOn = false;
			assert isAssertOn = true;
			if (isAssertOn) {
				System.out.printf("READY %s %s\n", this.getClass().getSimpleName(), this);
			}
		}
	}

	class IrpException extends IrpBase {
		Exception m_exception;

		public IrpException(Exception ex) {
			super(null);
			m_exception = ex;
		}
	}

	class IrpCommandFromDebuggee extends IrpBase {
		public IrpCommandFromDebuggee(Packet command) {
			super(command);
		}

		@Override
		void prepare() throws Exception {
			super.prepare();

			IrpCommandToDebuggee cmdIdSizes = m_cache.get(new JvmId(JDWP.Command.VIRTUAL_MACHINE_ID_SIZES));
			if (cmdIdSizes.getReplyPacket() == null) {
				subscribe(cmdIdSizes);
			} else {
				alert();
			}
		}

		@Override
		public void onready() throws Exception {
			super.onready();

			Packet command = payload;
			assert command.code == JDWP.Command.EVENT_COMPOSITE;

			ByteBuffer buffer = ByteBuffer.wrap(command.data);
			buffer.order(ByteOrder.BIG_ENDIAN);
			byte suspendPolicy = m_serializer.readByte(buffer);
			int cntEvents = m_serializer.readInt(buffer);
			System.out.printf("Events (suspendPolicy=%d, count=%d)\n", suspendPolicy, cntEvents);
			for (int i = 0; i < cntEvents; i++) {
				byte eventKind = m_serializer.readByte(buffer);
				int requestId = m_serializer.readInt(buffer);
				switch (eventKind) {
				case JDWP.EventKind.SINGLE_STEP: // 1
					// TODO: under construction
					System.out.printf("    SingleStep: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.BREAKPOINT: // 2
					// TODO: under construction
					System.out.printf("    Breakpoint: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.EXCEPTION: // 4
					// TODO: under construction
					System.out.printf("    Exception: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.THREAD_START: // 6
				{
					JvmId threadId = m_serializer.readObjectId(buffer);
					System.out.printf("    ThreadStart: requestId=%d, threadId=%s\n", requestId, threadId);
					break;
				}
				case JDWP.EventKind.THREAD_DEATH: // 7
				{
					JvmId threadId = m_serializer.readObjectId(buffer);
					System.out.printf("    ThreadDeath: requestId=%d, threadId=%s\n", requestId, threadId);
					break;
				}
				case JDWP.EventKind.CLASS_PREPARE: // 8
				{
					JvmId threadId = m_serializer.readObjectId(buffer);
					byte refTypeTag = m_serializer.readByte(buffer);
					JvmId typeId = m_serializer.readReferenceTypeId(buffer);
					String signature = m_serializer.readString(buffer);
					int status = m_serializer.readInt(buffer);
					System.out.printf("    ClassPrepare: requestId=%d, threadId=%s, refTypeTag=%d, typeId=%s, sig=%s, status=%d\n", requestId, threadId, refTypeTag, typeId, signature, status);
					break;
				}
				case JDWP.EventKind.CLASS_UNLOAD:  // 9
				{
					String signature = m_serializer.readString(buffer);
					System.out.printf("    ClassUnload: requestId=%d, sig=%s\n", requestId, signature);
					break;
				}
				case JDWP.EventKind.FIELD_ACCESS: // 20
					// TODO: under construction
					System.out.printf("    FieldAccess: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.FIELD_MODIFICATION: // 21
					// TODO: under construction
					System.out.printf("    FieldModification: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.METHOD_ENTRY: // 40
					// TODO: under construction
					System.out.printf("    MethodEntry: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.METHOD_EXIT: // 41
					// TODO: under construction
					System.out.printf("    MethodExit: requestId=%d\n", requestId);
					break;
				case JDWP.EventKind.VM_START: // 90
				{
					JvmId threadId = m_serializer.readObjectId(buffer);
					System.out.printf("    VMStart: requestId=%d, threadId=%s\n", requestId, threadId);
					break;
				}
				case JDWP.EventKind.VM_DEATH: // 99
				{
					System.out.printf("    VMDeath: requestId=%d\n", requestId);
					m_status = JDWP_DEBUGGEE_ACTIVE_CLOSE;
					break;
				}
				default:
					// TODO: under construction
					System.err.printf("received an unknown event from debuggee (eventKind=%d), bypass...\n", eventKind);
					assert false : "received an unknown event from debuggee";
				}
			}
			IrpCommandToDebugger commandIrp = new IrpCommandToDebugger(command);
			commandIrp.alert();
		}
	}

	class IrpCommandFromDebugger extends IrpBase {
		public IrpCommandFromDebugger(Packet command) {
			super(command);
		}

		public Packet getReplyPacket() {
			IrpCommandToDebuggee command = null;
			for (IRP dependant : getDependantsSnapshot()) {
				assert command == null;
				command = (IrpCommandToDebuggee)dependant;
			}
			assert command != null;
			return command.getReplyPacket();
		}

		@Override
		void prepare() throws Exception {
			super.prepare();

			Packet command = payload;
			JvmId commandKey = new JvmId(command.code);
			switch (command.code) {
			case JDWP.Command.THREAD_REFERENCE_NAME:
				commandKey = new JvmId(commandKey, command.data);
				break;
			}
			IrpCommandToDebuggee irp = m_cache.get(commandKey);
			if (irp == null) {
				irp = dispatchCommand(command);
				switch (command.code) {
				case JDWP.Command.VIRTUAL_MACHINE_CAPABILITIES:
				case JDWP.Command.VIRTUAL_MACHINE_CAPABILITIES_NEW:
				case JDWP.Command.THREAD_REFERENCE_NAME:
				case JDWP.Command.THREAD_REFERENCE_STATUS: // TODO: this hack needs to be removed, use timer and notification instead
					m_cache.put(commandKey, irp);
					break;
				}
			}
			subscribe(irp);
			if (irp.isReady()) {
				alert();
			}
		}

		@Override
		public void onready() throws Exception {
			super.onready();

			Packet command = payload;
			Packet replyFromDebuggee = getReplyPacket();
			Packet reply = Packet.createReply(command.id, replyFromDebuggee.code, replyFromDebuggee.data);
			IrpReplyToDebugger replyIrp = new IrpReplyToDebugger(reply);
			switch (command.code) {
			case JDWP.Command.VIRTUAL_MACHINE_DISPOSE:
				if (reply.code == JDWP.Error.NONE) {
					m_status = JDWP_DEBUGGER_ACTIVE_CLOSE;
				}
			}
			replyIrp.alert();
		}
	}

	class IrpCommandToDebuggee extends IrpBase {
		public IrpCommandToDebuggee(Packet command) {
			super(command);
		}

		public Packet getReplyPacket() {
			IRP[] dependantsSnapshot = getDependantsSnapshot();
			if (dependantsSnapshot != null) {
				for (IRP dependant : dependantsSnapshot) {
					return ((IrpReplyFromDebuggee)dependant).payload;
				}
			}
			return null;
		}

		@Override
		public void onready() throws Exception {
			super.onready();

			Packet command = payload;
			Packet reply = getReplyPacket();
			switch (command.code) {
			case JDWP.Command.VIRTUAL_MACHINE_ID_SIZES:
				if (m_serializer == null) {
					assert reply.code == 0;
					ByteBuffer buffer = ByteBuffer.wrap(reply.data);
					buffer.order(ByteOrder.BIG_ENDIAN);
					int fieldIdSize = Serializer.readInt(buffer);
					int methodIdSize = Serializer.readInt(buffer);
					int objectIdSize = Serializer.readInt(buffer);
					int referenceTypeIdSize = Serializer.readInt(buffer);
					int frameIdSize = Serializer.readInt(buffer);
					m_serializer = new Serializer(fieldIdSize, methodIdSize, objectIdSize, referenceTypeIdSize, frameIdSize);
				}
				break;
			}
		}
	}

	class IrpCommandToDebugger extends IrpBase {
		public IrpCommandToDebugger(Packet command) {
			super(command);
		}

		@Override
		public void onready() throws Exception {
			super.onready();
			payload.writeTo(m_incomingEndpoint);
		}
	}

	class IrpReplyFromDebuggee extends IrpBase {
		public IrpReplyFromDebuggee(Packet reply) {
			super(reply);
		}

		@Override
		void prepare() throws Exception {
			super.prepare();

			IrpCommandToDebuggee commandIrp = m_dispatchedCommands.get(payload.id);
			assert commandIrp != null; // REPLY should map to a previously sent COMMAND
			commandIrp.subscribe(this);
			alert();
		}
	}

	class IrpReplyToDebugger extends IrpBase {
		public IrpReplyToDebugger(Packet reply) {
			super(reply);
		}

		@Override
		public void onready() throws Exception {
			super.onready();

			payload.writeTo(m_incomingEndpoint);
		}
	}

	/*
	class ThreadInfo {
		public final long id;
		public final String name;
		public int status = JDWP.ThreadStatus.ZOMBIE;
		public int suspendStatus = 0;
		public ThreadInfo(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	*/
}
