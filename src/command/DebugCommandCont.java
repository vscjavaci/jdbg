package jdbg;

public class DebugCommandCont extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandCont();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
