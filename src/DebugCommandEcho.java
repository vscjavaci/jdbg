package jdbg;

public class DebugCommandEcho extends DebugCommand {
	@Override
	public int execute(DebugEngine engine) {
		System.out.println(this.getArguments().isEmpty() ? "" : this.getArguments().substring(1));
		return DebugEngine.JDBG_COMMAND_SUCCEEDED;
	}
}
