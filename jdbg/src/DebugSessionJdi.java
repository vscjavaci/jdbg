package jdbg;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.jdi.connect.*;

public class DebugSessionJdi extends DebugSession {
    Thread thread;
    volatile boolean connected = true;
    boolean completed = false;
    String shutdownMessageKey;
    boolean stopOnVMStart;

    @Override
    synchronized public void open(boolean stopOnVMStart) {
        if (null != this.thread) {
            return;
        }
        this.stopOnVMStart = stopOnVMStart;
        this.thread = new Thread(this, "event-handler");
        this.thread.start();
    }

    @Override
    synchronized public void close() {
        if (null == this.thread) {
            return;
        }
        connected = false;  // force run() loop termination
        thread.interrupt();
        while (!completed) {
            try {wait();} catch (InterruptedException exc) {}
        }
    }

    @Override
    public void run() {
        EventQueue queue = Env.vm().eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                boolean resumeStoppedApp = false;
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    resumeStoppedApp |= !handleEvent(it.nextEvent());
                }

                if (resumeStoppedApp) {
                    eventSet.resume();
                } else if (eventSet.suspendPolicy() == EventRequest.SUSPEND_ALL) {
                    setCurrentThread(eventSet);
                    onVMInterrupted();
                }
            } catch (InterruptedException exc) {
                // Do nothing. Any changes will be seen at top of loop.
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
        synchronized (this) {
            completed = true;
            notifyAll();
        }
    }

    private boolean handleEvent(Event event) {
        if (event instanceof BreakpointEvent) {
            return onBreakpointEvent((BreakpointEvent)event);
        }
        if (event instanceof ClassPrepareEvent) {
            return onClassPrepareEvent((ClassPrepareEvent)event);
        }
        if (event instanceof ClassUnloadEvent) {
            return onClassUnloadEvent((ClassUnloadEvent)event);
        }
        if (event instanceof ExceptionEvent) {
            return onExceptionEvent((ExceptionEvent)event);
        }
        if (event instanceof MethodEntryEvent) {
            return onMethodEntryEvent((MethodEntryEvent)event);
        }
        if (event instanceof MethodExitEvent) {
            return onMethodExitEvent((MethodExitEvent)event);
        }
        if (event instanceof StepEvent) {
            return onStepEvent((StepEvent)event);
        }
        if (event instanceof ThreadDeathEvent) {
            return onThreadDeathEvent((ThreadDeathEvent)event);
        }
        if (event instanceof ThreadStartEvent) {
            return onThreadStartEvent((ThreadStartEvent)event);
        }
        if (event instanceof WatchpointEvent) {
            return onWatchpointEvent((WatchpointEvent)event);
        }
        if (event instanceof VMStartEvent) {
            onVMStartEvent((VMStartEvent)event);
            return stopOnVMStart;
        }
        return handleExitEvent(event);
    }

    private boolean vmDied = false;
    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            shutdownMessageKey = "The application exited";
            vmDied = true;
            return onVMDeathEvent((VMDeathEvent)event);
        } else if (event instanceof VMDisconnectEvent) {
            connected = false;
            if (!vmDied) {
                shutdownMessageKey = "The application has been disconnected";
                onVMDisconnectEvent((VMDisconnectEvent)event);
            }
            Env.shutdown(shutdownMessageKey);
            return false;
        } else {
            throw new InternalError(MessageOutput.format("Unexpected event type",
                                                         new Object[] {event.getClass()}));
        }
    }

    synchronized void handleDisconnectedException() {
        /*
         * A VMDisconnectedException has happened while dealing with
         * another event. We need to flush the event queue, dealing only
         * with exit events (VMDeath, VMDisconnect) so that we terminate
         * correctly.
         */
        EventQueue queue = Env.vm().eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator iter = eventSet.eventIterator();
                while (iter.hasNext()) {
                    handleExitEvent(iter.next());
                }
            } catch (InterruptedException exc) {
                // ignore
            } catch (InternalError exc) {
                // ignore
            }
        }
    }

    private ThreadReference eventThread(Event event) {
        if (event instanceof ClassPrepareEvent) {
            return ((ClassPrepareEvent)event).thread();
        } else if (event instanceof LocatableEvent) {
            return ((LocatableEvent)event).thread();
        } else if (event instanceof ThreadStartEvent) {
            return ((ThreadStartEvent)event).thread();
        } else if (event instanceof ThreadDeathEvent) {
            return ((ThreadDeathEvent)event).thread();
        } else if (event instanceof VMStartEvent) {
            return ((VMStartEvent)event).thread();
        } else {
            return null;
        }
    }

    private void setCurrentThread(EventSet set) {
        ThreadReference thread;
        if (set.size() > 0) {
            /*
             * If any event in the set has a thread associated with it,
             * they all will, so just grab the first one.
             */
            Event event = set.iterator().next(); // Is there a better way?
            thread = eventThread(event);
        } else {
            thread = null;
        }
        setCurrentThread(thread);
    }

    private void setCurrentThread(ThreadReference thread) {
        ThreadInfo.invalidateAll();
        ThreadInfo.setCurrentThread(thread);
    }

    @Override
    public void onVMStartEvent(VMStartEvent event) {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("VM Started:");
    }

    @Override
    public boolean onBreakpointEvent(BreakpointEvent event) {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("Breakpoint hit:");
        return true;
    }

    @Override
    public boolean onWatchpointEvent(WatchpointEvent event) {
        Field field = event.field();
        ObjectReference obj = event.object();
        Thread.yield();  // fetch output

        if (event instanceof ModificationWatchpointEvent) {
            MessageOutput.lnprint("Field access encountered before after",
                                  new Object [] {field,
                                                 event.valueCurrent(),
                                                 ((ModificationWatchpointEvent)event).valueToBe()});
        } else {
            MessageOutput.lnprint("Field access encountered", field.toString());
        }
        return true;
    }

    @Override
    public boolean onStepEvent(StepEvent event) {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("Step completed:");
        return true;
    }

    @Override
    public boolean onExceptionEvent(ExceptionEvent event) {
        Thread.yield();  // fetch output
        Location catchLocation = event.catchLocation();
        if (catchLocation == null) {
            MessageOutput.lnprint("Exception occurred uncaught",
                                  event.exception().referenceType().name());
        } else {
            MessageOutput.lnprint("Exception occurred caught",
                                  new Object [] {event.exception().referenceType().name(),
                                                 Commands.locationString(catchLocation)});
        }
        return true;
    }

    @Override
    public boolean onMethodEntryEvent(MethodEntryEvent event) {
        Thread.yield();  // fetch output
        /*
         * These can be very numerous, so be as efficient as possible.
         * If we are stopping here, then we will see the normal location
         * info printed.
         */
        if (event.request().suspendPolicy() != EventRequest.SUSPEND_NONE) {
            // We are stopping; the name will be shown by the normal mechanism
            MessageOutput.lnprint("Method entered:");
        } else {
            // We aren't stopping, show the name
            MessageOutput.print("Method entered:");
            printLocationOfEvent(event);
        }
        return true;
    }

    @Override
    public boolean onMethodExitEvent(MethodExitEvent event) {
        Thread.yield();  // fetch output
        /*
         * These can be very numerous, so be as efficient as possible.
         */
        Method mmm = Env.atExitMethod();
        Method meMethod = event.method();

        if (mmm == null || mmm.equals(meMethod)) {
            // Either we are not tracing a specific method, or we are
            // and we are exitting that method.

            if (event.request().suspendPolicy() != EventRequest.SUSPEND_NONE) {
                // We will be stopping here, so do a newline
                MessageOutput.println();
            }
            if (Env.vm().canGetMethodReturnValues()) {
                MessageOutput.print("Method exitedValue:", event.returnValue() + "");
            } else {
                MessageOutput.print("Method exited:");
            }

            if (event.request().suspendPolicy() == EventRequest.SUSPEND_NONE) {
                // We won't be stopping here, so show the method name
                printLocationOfEvent(event);

            }

            // In case we want to have a one shot trace exit some day, this
            // code disables the request so we don't hit it again.
            if (false) {
                // This is a one shot deal; we don't want to stop
                // here the next time.
                Env.setAtExitMethod(null);
                EventRequestManager erm = Env.vm().eventRequestManager();
                for (EventRequest eReq : erm.methodExitRequests()) {
                    if (eReq.equals(event.request())) {
                        eReq.disable();
                    }
                }
            }
            return true;
        }

        // We are tracing a specific method, and this isn't it.  Keep going.
        return false;
    }

    @Override
    public void onVMInterrupted() {
        Thread.yield(); // fetch output
        printCurrentLocation();
        MessageOutput.printPrompt();
    }

    private void printBaseLocation(String threadName, Location loc) {
        MessageOutput.println("location",
                              new Object [] {threadName,
                                             Commands.locationString(loc)});
    }

    private void printCurrentLocation() {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        StackFrame frame;
        try {
            frame = threadInfo.getCurrentFrame();
        } catch (IncompatibleThreadStateException exc) {
            MessageOutput.println("<location unavailable>");
            return;
        }
        if (frame == null) {
            MessageOutput.println("No frames on the current call stack");
        } else {
            Location loc = frame.location();
            printBaseLocation(threadInfo.getThread().name(), loc);
            // Output the current source line, if possible
            if (loc.lineNumber() != -1) {
                String line;
                try {
                    line = Env.sourceLine(loc, loc.lineNumber());
                } catch (java.io.IOException e) {
                    line = null;
                }
                if (line != null) {
                    MessageOutput.println("source line number and line",
                                          new Object [] {new Integer(loc.lineNumber()),
                                                         line});
                }
            }
        }
        MessageOutput.println();
    }

    private void printLocationOfEvent(LocatableEvent event) {
        printBaseLocation(event.thread().name(), event.location());
    }

    public DebugSessionJdi() throws Exception {
        if (Env.connection().isOpen() && Env.vm().canBeModified()) {
            /*
             * Connection opened on startup. Start event handler
             * immediately, telling it (through arg 2) to stop on the
             * VM start event.
             */
            open(true);
        }
    }
}
