package jdbg;

import java.util.StringTokenizer;

public class DebugCommandSleep extends DebugCommand {
	@Override
	public int execute(DebugEngine engine) {
		long millis = 1000;
		StringTokenizer st = new StringTokenizer(this.getArguments());
		if (st.hasMoreTokens()) {
			millis = Long.parseLong(st.nextToken(), 10);
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// silently ignore
		}
		return DebugEngine.JDBG_COMMAND_SUCCEEDED;
	}
}
