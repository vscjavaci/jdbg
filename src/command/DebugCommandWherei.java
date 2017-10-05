package jdbg;

import java.util.StringTokenizer;

public class DebugCommandWherei extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandWhere(st, true);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
