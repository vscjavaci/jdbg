package jdbg;

import com.sun.jdi.Bootstrap;

public class DebugCommandVersion extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandVersion(MainClass.progname, Bootstrap.virtualMachineManager());
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
