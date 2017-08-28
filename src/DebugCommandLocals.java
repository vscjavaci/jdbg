package jdbg;

public class DebugCommandLocals extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandLocals();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
