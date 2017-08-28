package jdbg;

public class DebugCommandGC extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandGC();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
