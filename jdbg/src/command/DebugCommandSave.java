package jdbg;

import java.util.StringTokenizer;

public class DebugCommandSave extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandSave(st);
        // showPrompt = false;        // asynchronous command
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
