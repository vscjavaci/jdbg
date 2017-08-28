package jdbg;

import java.util.StringTokenizer;

public class DebugCommandReenter extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandPopFrames(st, true);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
