package jdbg;

import java.util.StringTokenizer;

public class DebugCommandExtension extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandExtension(st);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
