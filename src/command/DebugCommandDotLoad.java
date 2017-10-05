package jdbg;

import java.util.StringTokenizer;

class DebugCommandDotLoad extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        StringTokenizer st = new StringTokenizer(this.getArguments());
        try {
            registerCommand(st.nextToken(), st.nextToken());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return DebugEngine.JDBG_COMMAND_FAILED;
        }
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
