package jdbg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class DebugCommand {
	private String             m_name;
	private String             m_arguments;
	private String             m_filePath;
	private int                m_lineNumber;
	private int                m_repeatCount;
	private List<DebugCommand> m_childCommands;
	private DebugCommand       m_parentCommand;

	public String              getName() { return m_name; }
	public String              getArguments() { return m_arguments; }
	public int                 getRepeatCount() { return m_repeatCount; }
	public DebugCommand        getParentCommand() { return m_parentCommand; }
	public List<DebugCommand>  getChildCommands() { return m_childCommands; }

	private static Map<String, Class> commandRegistry = new HashMap<String, Class>();

	static {
		commandRegistry.put(".load", DebugCommandDotLoad.class); // load command extension
		commandRegistry.put(".run", DebugCommandDotRun.class); // run script
	}

	protected static void registerCommand(String name, String className) throws ClassNotFoundException {
		ClassLoader classLoader = DebugCommand.class.getClassLoader();
		Class aClass = classLoader.loadClass(className);
		commandRegistry.put(name, aClass);
	}

	public void appendChildCommand(DebugCommand command) {
		if (null == m_childCommands) {
			m_childCommands = new LinkedList<DebugCommand>();
		}
		m_childCommands.add(command);
	}

	public boolean isCommand(String command) {
		return command.equals(m_name);
	}

	public static DebugCommand Create(String line) {
		return Create(line, "<unknown>", -1);
	}

	public static DebugCommand Create(String line, String filePath, int lineNumber) {
		return Create(line, filePath, lineNumber, null);
	}

	public static DebugCommand Create(String line, String filePath, int lineNumber, DebugCommand parentCommand) {
		DebugCommand retval = null;
		if (line.trim().startsWith("#")) {
			return retval;
		}
		StringTokenizer st = new StringTokenizer(line);
		if (!st.hasMoreTokens()) {
			return retval;
		}
		String name = st.nextToken();
		int repeatCount = 1;
		try {
			repeatCount = Integer.parseInt(name);
			if (repeatCount > 0 && st.hasMoreTokens()) {
				name = st.nextToken();
			} else {
				repeatCount = 1;
			}
		} catch (NumberFormatException ex) {
		}
		String arguments = st.hasMoreTokens() ? st.nextToken("") : "";
		try {
			if (commandRegistry.containsKey(name)) {
				retval = (DebugCommand)commandRegistry.get(name).newInstance();
			} else {
				retval = new DebugCommand();
			}
		} catch (IllegalAccessException | InstantiationException | NullPointerException ex) {
			return retval;
		}
		retval.m_name = name;
		retval.m_arguments = arguments;
		retval.m_repeatCount = repeatCount;
		retval.m_parentCommand = parentCommand;
		retval.m_filePath = filePath;
		retval.m_lineNumber = lineNumber;
		return retval;
	}

	public int execute(DebugEngine engine) {
		System.err.format("ERROR: unknown command \"%s%s\" at %s:L%d\n", m_name, m_arguments, m_filePath, m_lineNumber);
		return DebugEngine.JDBG_COMMAND_FAILED;
	}
}
