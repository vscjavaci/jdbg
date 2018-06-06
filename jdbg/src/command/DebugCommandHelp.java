package jdbg;

public class DebugCommandHelp extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        MessageOutput.println("zz help text");
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
