package jdbg;

public class DebugCommandStepi extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandStepi();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
