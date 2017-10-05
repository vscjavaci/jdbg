package jdbg;

import java.util.StringTokenizer;

public class DebugCommandClass extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandClass(st);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
