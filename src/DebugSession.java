package jdbg;

import com.sun.jdi.event.*;

public abstract class DebugSession {
    public boolean breakpointEvent(BreakpointEvent event) { return true; }
    public boolean classPrepareEvent(ClassPrepareEvent event) {
        if (!Env.specList.resolve(event)) {
            MessageOutput.lnprint("Stopping due to deferred breakpoint errors.");
            return true;
        } else {
            return false;
        }
    }

    public boolean classUnloadEvent(ClassUnloadEvent event) { return false; }
    public boolean exceptionEvent(ExceptionEvent event) { return true; }
    public boolean fieldWatchEvent(WatchpointEvent event) { return true; }
    public boolean methodEntryEvent(MethodEntryEvent event) { return false; }
    public boolean methodExitEvent(MethodExitEvent event) { return false; }
    public boolean stepEvent(StepEvent event) { return true; }
    public boolean threadDeathEvent(ThreadDeathEvent event) { return false; }
    public boolean threadStartEvent(ThreadStartEvent event) { return false; }
    public boolean vmDeathEvent(VMDeathEvent event) { return false; }
    public boolean vmDisconnectEvent(VMDisconnectEvent event) { return false; }
    public abstract void vmStartEvent(VMStartEvent event);

    public void vmInterrupted() {}
}
