package jdbg;

public class DebugCommandClasses extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandClasses();
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
