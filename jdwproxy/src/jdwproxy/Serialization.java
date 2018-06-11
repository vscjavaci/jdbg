package jdwproxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

class JvmId {
	public final byte[] data;
	private final Long m_value;

	protected JvmId(byte [] data) {
		this.data = data;
		long value = 0;
		for (int i = 0; i < data.length; i++) {
			value = (value << 8) | data[i];
		}
		m_value = value;
	}

	protected JvmId(short n) {
		this(new byte[] {(byte)(n >> 8), (byte)n});
	}

	protected JvmId(int n) {
		this(new byte[] {(byte)(n >> 24), (byte)(n >> 16), (byte)(n >> 8), (byte)n});
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	protected JvmId(JvmId id, byte[] data) {
		this(concat(id.data, data));
	}

	long value() {
		return m_value;
	}

	@Override
	public boolean equals(Object o) {
	if (o == this) {
		return true;
	}
	JvmId other = o instanceof JvmId ? (JvmId)o : null;
	if (other != null && other.data.length == this.data.length) {
		return m_value.equals(other.m_value);
	}
	return false;
  }

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

	@Override
	public String toString() {
		String retval = "";
		for (byte b : data) {
			retval += String.format("%02x", b);
		}
		return retval;
	}
}

class Packet {
	static final byte COMMAND = 0;
	static final byte REPLY = (byte)0x80;
	static final int SIZE_OF_PACKET_HEADER = 11;

	// JDWP is using big-endian
	public int id;
	public byte flags;

	short code; // commandSet:command or errorCode, depending on the flags

	byte[] data;

	@Override
	public String toString() {
		String retval;
		if (flags == COMMAND) {
			retval = String.format("Command [#%d] cmd=%d:%d", id, code >> 8, code & 0xff);
		} else if (flags == REPLY) {
			retval = String.format("Reply [#%d] err=%d", id, code);
		} else {
			retval = String.format("Unknown [#%d] flags=%d code=%d", id, flags, code);
		}
		if (data != null) {
			for (byte b : data) {
				if (retval.length() >= 74) {
					retval += "...";
					break;
				}
				retval += String.format(" %02x", b);
			}
		}
		return retval;
	}

	Packet(int _id, byte _flags) {
		id = _id;
		flags = _flags;
	}

	public Packet(Endpoint endpoint) throws IOException {
		readFrom(endpoint);
	}

	public static Packet createCommand(int id, short code, byte[] data) {
		Packet retval = new Packet(id, COMMAND);
		retval.code = code;
		retval.data = data;
		return retval;
	}

	public static Packet createReply(int id, short errorCode, byte[] data) {
		Packet retval = new Packet(id, REPLY);
		retval.code = errorCode;
		retval.data = data;
		return retval;
	}

	public void readFrom(Endpoint endpoint) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(endpoint.read(SIZE_OF_PACKET_HEADER));
		buffer.order(ByteOrder.BIG_ENDIAN);

		int length = buffer.getInt();

		id = buffer.getInt();
		flags = buffer.get();
		code = buffer.getShort();
		length -= 11;
		if (length > 0) {
			data = endpoint.read(length);
		}
	}

	public void writeTo(Endpoint endpoint) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(SIZE_OF_PACKET_HEADER);
		buffer.order(ByteOrder.BIG_ENDIAN);

		int length = SIZE_OF_PACKET_HEADER;
		if (data != null) {
			length += data.length;
		}
		buffer.putInt(length);
		buffer.putInt(id);
		buffer.put(flags);
		buffer.putShort(code);
		endpoint.write(buffer.array());

		if (data != null) {
			endpoint.write(data);
		}
	}
}

class Serializer {
	private int m_fieldIdSize;
	private int m_methodIdSize;
	private int m_objectIdSize;
	private int m_referenceTypeIdSize;
	private int m_frameIdSize;

	public Serializer(int fieldIdSize, int methodIdSize, int objectIdSize, int referenceTypeIdSize, int frameIdSize) {
		this.m_fieldIdSize = fieldIdSize;
		this.m_methodIdSize = methodIdSize;
		this.m_objectIdSize = objectIdSize;
		this.m_referenceTypeIdSize = referenceTypeIdSize;
		this.m_frameIdSize = frameIdSize;
	}

	public static boolean readBoolean(ByteBuffer buffer) {
		return buffer.get() != 0;
	}

	public static byte readByte(ByteBuffer buffer) {
		return buffer.get();
	}

	public static int readInt(ByteBuffer buffer) {
		return buffer.getInt();
	}

	public static long readLong(ByteBuffer buffer) {
		return buffer.getLong();
	}

	public JvmId readFieldId(ByteBuffer buffer) {
		byte[] data = new byte[m_fieldIdSize];
		buffer.get(data);
		return new JvmId(data);
	}

	public JvmId readMethodId(ByteBuffer buffer) {
		byte[] data = new byte[m_methodIdSize];
		buffer.get(data);
		return new JvmId(data);
	}

	public JvmId readObjectId(ByteBuffer buffer) {
		byte[] data = new byte[m_objectIdSize];
		buffer.get(data);
		return new JvmId(data);
	}

	public JvmId readReferenceTypeId(ByteBuffer buffer) {
		byte[] data = new byte[m_referenceTypeIdSize];
		buffer.get(data);
		return new JvmId(data);
	}

	public JvmId readFrameId(ByteBuffer buffer) {
		byte[] data = new byte[m_frameIdSize];
		buffer.get(data);
		return new JvmId(data);
	}

	public static String readString(ByteBuffer buffer) {
		int length = readInt(buffer);
		byte[] str = new byte[length];
		buffer.get(str);
		try {
			return new String(str, "UTF-8");
		} catch (Exception ex) {
			return null;
		}
	}
}
