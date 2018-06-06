package jdbg;

import com.sun.jdi.event.*;

public abstract class DebugSession implements Runnable {
    public abstract void open(boolean stopOnVMStart);
    public abstract void close();

    public boolean onBreakpointEvent(BreakpointEvent event) { return true; }
    public boolean onClassPrepareEvent(ClassPrepareEvent event) {
        if (!Env.specList.resolve(event)) {
            MessageOutput.lnprint("Stopping due to deferred breakpoint errors.");
            return true;
        } else {
            return false;
        }
    }

    public boolean onClassUnloadEvent(ClassUnloadEvent event) { return false; }
    public boolean onExceptionEvent(ExceptionEvent event) { return true; }
    public boolean onMethodEntryEvent(MethodEntryEvent event) { return false; }
    public boolean onMethodExitEvent(MethodExitEvent event) { return false; }
    public boolean onStepEvent(StepEvent event) { return true; }
    public boolean onThreadDeathEvent(ThreadDeathEvent event) {
        ThreadInfo.removeThread(event.thread());
        return false;
    }
    public boolean onThreadStartEvent(ThreadStartEvent event) {
        ThreadInfo.addThread(event.thread());
        return false;
    }
    public boolean onVMDeathEvent(VMDeathEvent event) { return false; }
    public void onVMDisconnectEvent(VMDisconnectEvent event) {}
    public void onVMStartEvent(VMStartEvent event) {}
    public boolean onWatchpointEvent(WatchpointEvent event) { return true; }

    public void onVMInterrupted() {}
}
