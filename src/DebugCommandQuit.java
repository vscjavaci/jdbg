package jdbg;

public class DebugCommandQuit extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        if (Env.handler != null) {
            Env.handler.shutdown();
        }
        Env.shutdown();
        return DebugEngine.JDBG_COMMAND_QUIT;
    }
}
