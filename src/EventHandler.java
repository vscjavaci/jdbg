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
                    session.onVMInterrupted();
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
            return session.onBreakpointEvent((BreakpointEvent)event);
        }
        if (event instanceof ClassPrepareEvent) {
            return session.onClassPrepareEvent((ClassPrepareEvent)event);
        }
        if (event instanceof ClassUnloadEvent) {
            return session.onClassUnloadEvent((ClassUnloadEvent)event);
        }
        if (event instanceof ExceptionEvent) {
            return session.onExceptionEvent((ExceptionEvent)event);
        }
        if (event instanceof MethodEntryEvent) {
            return session.onMethodEntryEvent((MethodEntryEvent)event);
        }
        if (event instanceof MethodExitEvent) {
            return session.onMethodExitEvent((MethodExitEvent)event);
        }
        if (event instanceof StepEvent) {
            return session.onStepEvent((StepEvent)event);
        }
        if (event instanceof ThreadDeathEvent) {
            return session.onThreadDeathEvent((ThreadDeathEvent)event);
        }
        if (event instanceof ThreadStartEvent) {
            return session.onThreadStartEvent((ThreadStartEvent)event);
        }
        if (event instanceof WatchpointEvent) {
            return session.onWatchpointEvent((WatchpointEvent)event);
        }
        if (event instanceof VMStartEvent) {
            session.onVMStartEvent((VMStartEvent)event);
            return stopOnVMStart;
        }
        return handleExitEvent(event);
    }

    private boolean vmDied = false;
    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            shutdownMessageKey = "The application exited";
            vmDied = true;
            return session.onVMDeathEvent((VMDeathEvent)event);
        } else if (event instanceof VMDisconnectEvent) {
            connected = false;
            if (!vmDied) {
                shutdownMessageKey = "The application has been disconnected";
                session.onVMDisconnectEvent((VMDisconnectEvent)event);
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
}
