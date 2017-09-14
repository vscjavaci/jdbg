package jdbg;

import java.util.StringTokenizer;

public class DebugCommandRun extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        StringTokenizer st = new StringTokenizer(this.getArguments());
        evaluator.commandRun(st);
        /*
         * Fire up an event handler, if the connection was just
         * opened. Since this was done from the run command
         * we don't stop the VM on its VM start event (so
         * arg 2 is false).
         */
        if (Env.connection().isOpen()) {
            Env.session.open(false);
        }
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
