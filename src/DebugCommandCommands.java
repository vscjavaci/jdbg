package jdbg;

public class DebugCommandCommands extends DebugCommand {
	@Override
	public int execute(DebugEngine engine) {
		engine.appendCommands(this.getChildCommands());
		return DebugEngine.JDBG_COMMAND_SUCCEEDED;
	}
}
