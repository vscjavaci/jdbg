package jdbg;

import java.util.StringTokenizer;

public class DebugCommandThreadGroup extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandThreadGroup(st);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
