package jdbg;

import java.util.StringTokenizer;

public class DebugCommandWhere extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandWhere(st, false);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
