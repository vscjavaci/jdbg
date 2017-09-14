package jdbg;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.jdi.connect.*;

public class DebugSessionJdi extends DebugSession {
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
        Env.session = this;
        if (Env.connection().isOpen() && Env.vm().canBeModified()) {
            /*
             * Connection opened on startup. Start event handler
             * immediately, telling it (through arg 2) to stop on the
             * VM start event.
             */
            Env.handler = new EventHandler(Env.session, true);
        }
    }
}
