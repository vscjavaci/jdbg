package jdbg;

import com.sun.jdi.event.*;

public abstract class DebugSession {
    public abstract void vmStartEvent(VMStartEvent e);
    public abstract void vmDeathEvent(VMDeathEvent e);
    public abstract void vmDisconnectEvent(VMDisconnectEvent e);

    public abstract void threadStartEvent(ThreadStartEvent e);
    public abstract void threadDeathEvent(ThreadDeathEvent e);

    public abstract void classPrepareEvent(ClassPrepareEvent e);
    public abstract void classUnloadEvent(ClassUnloadEvent e);

    public abstract void breakpointEvent(BreakpointEvent e);
    public abstract void fieldWatchEvent(WatchpointEvent e);
    public abstract void stepEvent(StepEvent e);
    public abstract void exceptionEvent(ExceptionEvent e);
    public abstract void methodEntryEvent(MethodEntryEvent e);
    public abstract boolean methodExitEvent(MethodExitEvent e);

    public abstract void vmInterrupted();
}
