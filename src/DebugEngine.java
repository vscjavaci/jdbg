package jdbg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class DebugEngine implements Runnable {
	public static final int JDBG_UNKNOWN_ERROR = -0x7fffffff;
	public static final int JDBG_COMMAND_FAILED = -1;
	public static final int JDBG_COMMAND_SUCCEEDED = 0;
	public static final int JDBG_COMMAND_EMPTY = 1;
	public static final int JDBG_COMMAND_QUIT = 2;

	private int m_status = JDBG_COMMAND_EMPTY;
	private List<DebugCommand> commandQueue = new LinkedList<DebugCommand>();

	public DebugEngine() {
	}

	public synchronized void appendCommand(DebugCommand element) {
		commandQueue.add(element);
		this.notify();
	}

	public synchronized void appendCommands(Collection<DebugCommand> elements) {
		commandQueue.addAll(elements);
		this.notify();
	}

	public synchronized void prependCommand(DebugCommand element) {
		commandQueue.add(0, element);
		this.notify();
	}

	public synchronized void prependCommands(Collection<DebugCommand> elements) {
		commandQueue.addAll(0, elements);
		this.notify();
	}

	public int runScript(String script) {
		return this.runScript(script, "<init>", null);
	}

	public int runScript(String script, String filePath, DebugCommand parentCommand) {
		try {
			return this.runScript(new BufferedReader(new StringReader(script)), filePath, parentCommand);
		} catch (IOException ex) {
			return JDBG_UNKNOWN_ERROR;
		}
	}

	int runScript(BufferedReader reader, String filePath, DebugCommand parentCommand) throws IOException {
		int lineNumber = 0;
		String line = reader.readLine();
		List<DebugCommand> commands = new LinkedList<DebugCommand>();
		int nestingLevel = 0;
		while (null != line) {
			DebugCommand command = DebugCommand.Create(line, filePath, ++lineNumber, parentCommand);
			if (null != command) {
				if (command.isCommand("end")) {
					if (0 == nestingLevel) {
						return JDBG_COMMAND_FAILED; // "end" without "commands"
					}
					--nestingLevel;
					parentCommand = parentCommand.getParentCommand();
				} else {
					if (0 == nestingLevel) {
						commands.add(command);
					} else {
						parentCommand.appendChildCommand(command);
					}
					if (command.isCommand("commands")) {
						++nestingLevel;
						parentCommand = command;
					}
				}
			}
			line = reader.readLine();
		}
		if (0 != nestingLevel) {
			return JDBG_COMMAND_FAILED; // open "commands" without "end"
		}
		this.prependCommands(commands);
		return JDBG_COMMAND_SUCCEEDED;
	}

	private synchronized int sync() {
		try {
			while (0 != commandQueue.size() && JDBG_COMMAND_QUIT != m_status) {
				this.wait();
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		return m_status;
	}

	@Override
	public void run() {
		try {
			while (true) {
				synchronized (this) {
					while (0 == commandQueue.size()) {
						this.wait();
					}
					try {
						m_status = execute(commandQueue.remove(0));
					} catch (RuntimeException ex) {
						ex.printStackTrace();
						m_status = JDBG_UNKNOWN_ERROR;
					}
					if (JDBG_COMMAND_QUIT == m_status) {
						this.notify();
						break;
					}
					if (0 == commandQueue.size()) {
						this.notify();
					}
				}
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	public int loop() {
		Thread debugEngineThread = new Thread(this, "debug-engine-command-processing-loop");
		debugEngineThread.start();

		// run the initialization script
		this.sync();

		// run jdbg.ini file if it exists
		if (new File("jdbg.ini").exists()) {
			this.runScript(".run jdbg.ini");
			this.sync();
		}

		if (JDBG_COMMAND_QUIT == m_status) {
			return JDBG_COMMAND_QUIT;
		}

		InteractiveConsole console = new InteractiveConsole();
		Thread consoleInputThread = new Thread(console, "console-input-message-loop");
		consoleInputThread.start();
		try {
			int lineNumber = 0;
			while (true) {
				String line = console.getInput("> "); // TODO: this should be governed by "shouldPrompt"
				if (null == line) {
					line = "quit"; // end of standard input
				}
				DebugCommand command = DebugCommand.Create(line, "<stdin>", ++lineNumber);
				if (null == command) {
					continue;
				}
				this.appendCommand(command);
				this.sync();
				if (JDBG_COMMAND_QUIT == m_status) {
					break;
				}
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return JDBG_UNKNOWN_ERROR;
		} finally {
			console.close(); // close console input
		}
		try {
			consoleInputThread.join();
			debugEngineThread.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return JDBG_UNKNOWN_ERROR;
		}

		return m_status;
	}

	private int execute(DebugCommand cmd) {
		int retval = JDBG_COMMAND_SUCCEEDED;
		for (int i = 0; i < cmd.getRepeatCount(); i++) {
			retval = cmd.execute(this);
			if (JDBG_COMMAND_QUIT == retval) {
				break;
			}
		}
		return retval;
	}
}
