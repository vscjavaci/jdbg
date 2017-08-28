package jdbg;

public class DebugCommandMemory extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandMemory();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
