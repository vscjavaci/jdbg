package jdbg;

public class DebugCommandThreadGroups extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandThreadGroups();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
