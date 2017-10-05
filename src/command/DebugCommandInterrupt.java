package jdbg;

import java.util.StringTokenizer;

public class DebugCommandInterrupt extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandInterrupt(st);
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
