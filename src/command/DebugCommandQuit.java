package jdbg;

public class DebugCommandQuit extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Env.session.close();
        Env.shutdown();
        return DebugEngine.JDBG_COMMAND_QUIT;
    }
}
