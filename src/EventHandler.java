package jdbg;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

public class EventHandler implements Runnable {

    DebugSession session;
    Thread thread;
    volatile boolean connected = true;
    boolean completed = false;
    String shutdownMessageKey;
    boolean stopOnVMStart;

    EventHandler(DebugSession session, boolean stopOnVMStart) {
        this.session = session;
        this.stopOnVMStart = stopOnVMStart;
        this.thread = new Thread(this, "event-handler");
        this.thread.start();
    }

    synchronized void shutdown() {
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
                    session.vmInterrupted();
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
        if (event instanceof ExceptionEvent) {
            return exceptionEvent(event);
        } else if (event instanceof BreakpointEvent) {
            return breakpointEvent(event);
        } else if (event instanceof WatchpointEvent) {
            return fieldWatchEvent(event);
        } else if (event instanceof StepEvent) {
            return stepEvent(event);
        } else if (event instanceof MethodEntryEvent) {
            return methodEntryEvent(event);
        } else if (event instanceof MethodExitEvent) {
            return methodExitEvent(event);
        } else if (event instanceof ClassPrepareEvent) {
            return classPrepareEvent(event);
        } else if (event instanceof ClassUnloadEvent) {
            return classUnloadEvent(event);
        } else if (event instanceof ThreadStartEvent) {
            return threadStartEvent(event);
        } else if (event instanceof ThreadDeathEvent) {
            return threadDeathEvent(event);
        } else if (event instanceof VMStartEvent) {
            return vmStartEvent(event);
        } else {
            return handleExitEvent(event);
        }
    }

    private boolean vmDied = false;
    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            vmDied = true;
            return vmDeathEvent(event);
        } else if (event instanceof VMDisconnectEvent) {
            connected = false;
            if (!vmDied) {
                vmDisconnectEvent(event);
            }
            /*
             * Inform jdb command line processor that jdb is being shutdown. JDK-8154144.
             */
            ((DebugSessionJdi)session).setShuttingDown(true);
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

    private boolean vmStartEvent(Event event)  {
        VMStartEvent se = (VMStartEvent)event;
        session.vmStartEvent(se);
        return stopOnVMStart;
    }

    private boolean breakpointEvent(Event event)  {
        BreakpointEvent be = (BreakpointEvent)event;
        session.breakpointEvent(be);
        return true;
    }

    private boolean methodEntryEvent(Event event)  {
        MethodEntryEvent me = (MethodEntryEvent)event;
        session.methodEntryEvent(me);
        return true;
    }

    private boolean methodExitEvent(Event event)  {
        MethodExitEvent me = (MethodExitEvent)event;
        return session.methodExitEvent(me);
    }

    private boolean fieldWatchEvent(Event event)  {
        WatchpointEvent fwe = (WatchpointEvent)event;
        session.fieldWatchEvent(fwe);
        return true;
    }

    private boolean stepEvent(Event event)  {
        StepEvent se = (StepEvent)event;
        session.stepEvent(se);
        return true;
    }

    private boolean classPrepareEvent(Event event)  {
        ClassPrepareEvent cle = (ClassPrepareEvent)event;
        session.classPrepareEvent(cle);

        if (!Env.specList.resolve(cle)) {
            MessageOutput.lnprint("Stopping due to deferred breakpoint errors.");
            return true;
        } else {
            return false;
        }
    }

    private boolean classUnloadEvent(Event event)  {
        ClassUnloadEvent cue = (ClassUnloadEvent)event;
        session.classUnloadEvent(cue);
        return false;
    }

    private boolean exceptionEvent(Event event) {
        ExceptionEvent ee = (ExceptionEvent)event;
        session.exceptionEvent(ee);
        return true;
    }

    private boolean threadDeathEvent(Event event) {
        ThreadDeathEvent tee = (ThreadDeathEvent)event;
        ThreadInfo.removeThread(tee.thread());
        return false;
    }

    private boolean threadStartEvent(Event event) {
        ThreadStartEvent tse = (ThreadStartEvent)event;
        ThreadInfo.addThread(tse.thread());
        session.threadStartEvent(tse);
        return false;
    }

    public boolean vmDeathEvent(Event event) {
        shutdownMessageKey = "The application exited";
        session.vmDeathEvent((VMDeathEvent)event);
        return false;
    }

    public boolean vmDisconnectEvent(Event event) {
        shutdownMessageKey = "The application has been disconnected";
        session.vmDisconnectEvent((VMDisconnectEvent)event);
        return false;
    }
}
