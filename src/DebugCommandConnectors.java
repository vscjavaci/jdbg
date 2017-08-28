package jdbg;

import com.sun.jdi.Bootstrap;

public class DebugCommandConnectors extends DebugCommand {
    @Override
    public int execute(DebugEngine engine) {
        Commands evaluator = new Commands();
        evaluator.commandConnectors(Bootstrap.virtualMachineManager());
        return DebugEngine.JDBG_COMMAND_SUCCEEDED;
    }
}
