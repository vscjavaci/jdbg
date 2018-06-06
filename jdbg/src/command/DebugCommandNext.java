package jdbg;

public class DebugCommandNext extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandNext();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
