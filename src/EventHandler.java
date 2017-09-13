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
            return exceptionEvent((ExceptionEvent)event);
        } else if (event instanceof BreakpointEvent) {
            return breakpointEvent((BreakpointEvent)event);
        } else if (event instanceof WatchpointEvent) {
            return fieldWatchEvent((WatchpointEvent)event);
        } else if (event instanceof StepEvent) {
            return stepEvent((StepEvent)event);
        } else if (event instanceof MethodEntryEvent) {
            return methodEntryEvent((MethodEntryEvent)event);
        } else if (event instanceof MethodExitEvent) {
            return methodExitEvent((MethodExitEvent)event);
        } else if (event instanceof ClassPrepareEvent) {
            return classPrepareEvent((ClassPrepareEvent)event);
        } else if (event instanceof ClassUnloadEvent) {
            return classUnloadEvent((ClassUnloadEvent)event);
        } else if (event instanceof ThreadStartEvent) {
            return threadStartEvent((ThreadStartEvent)event);
        } else if (event instanceof ThreadDeathEvent) {
            return threadDeathEvent((ThreadDeathEvent)event);
        } else if (event instanceof VMStartEvent) {
            return vmStartEvent((VMStartEvent)event);
        } else {
            return handleExitEvent(event);
        }
    }

    private boolean vmDied = false;
    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            vmDied = true;
            return vmDeathEvent((VMDeathEvent)event);
        } else if (event instanceof VMDisconnectEvent) {
            connected = false;
            if (!vmDied) {
                vmDisconnectEvent((VMDisconnectEvent)event);
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

    private boolean vmStartEvent(VMStartEvent event)  {
        session.vmStartEvent(event);
        return stopOnVMStart;
    }

    private boolean breakpointEvent(BreakpointEvent event)  {
        return session.breakpointEvent(event);
    }

    private boolean methodEntryEvent(Event event)  {
        MethodEntryEvent me = (MethodEntryEvent)event;
        return session.methodEntryEvent(me);
    }

    private boolean methodExitEvent(MethodExitEvent event)  {
        return session.methodExitEvent(event);
    }

    private boolean fieldWatchEvent(WatchpointEvent event)  {
        return session.fieldWatchEvent(event);
    }

    private boolean stepEvent(Event event)  {
        StepEvent se = (StepEvent)event;
        return session.stepEvent(se);
    }

    private boolean classPrepareEvent(ClassPrepareEvent event)  {
        return session.classPrepareEvent(event);
    }

    private boolean classUnloadEvent(ClassUnloadEvent event)  {
        return session.classUnloadEvent(event);
    }

    private boolean exceptionEvent(ExceptionEvent event) {
        return session.exceptionEvent(event);
    }

    private boolean threadDeathEvent(ThreadDeathEvent event) {
        ThreadInfo.removeThread(event.thread());
        return session.threadDeathEvent(event);
    }

    private boolean threadStartEvent(Event event) {
        ThreadStartEvent tse = (ThreadStartEvent)event;
        ThreadInfo.addThread(tse.thread());
        return session.threadStartEvent(tse);
    }

    public boolean vmDeathEvent(VMDeathEvent event) {
        shutdownMessageKey = "The application exited";
        return session.vmDeathEvent(event);
    }

    public boolean vmDisconnectEvent(VMDisconnectEvent event) {
        shutdownMessageKey = "The application has been disconnected";
        return session.vmDisconnectEvent(event);
    }
}
