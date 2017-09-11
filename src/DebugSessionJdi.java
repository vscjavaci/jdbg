package jdbg;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.jdi.connect.*;

public class DebugSessionJdi extends DebugSession {
    private volatile boolean shuttingDown = false;

    public void setShuttingDown(boolean s) {
       shuttingDown = s;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    @Override
    public void vmStartEvent(VMStartEvent se)  {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("VM Started:");
    }

    @Override
    public void vmDeathEvent(VMDeathEvent e)  {
    }

    @Override
    public void vmDisconnectEvent(VMDisconnectEvent e)  {
    }

    @Override
    public void threadStartEvent(ThreadStartEvent e)  {
    }

    @Override
    public void threadDeathEvent(ThreadDeathEvent e)  {
    }

    @Override
    public void classPrepareEvent(ClassPrepareEvent e)  {
    }

    @Override
    public void classUnloadEvent(ClassUnloadEvent e)  {
    }

    @Override
    public void breakpointEvent(BreakpointEvent be)  {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("Breakpoint hit:");
    }

    @Override
    public void fieldWatchEvent(WatchpointEvent fwe)  {
        Field field = fwe.field();
        ObjectReference obj = fwe.object();
        Thread.yield();  // fetch output

        if (fwe instanceof ModificationWatchpointEvent) {
            MessageOutput.lnprint("Field access encountered before after",
                                  new Object [] {field,
                                                 fwe.valueCurrent(),
                                                 ((ModificationWatchpointEvent)fwe).valueToBe()});
        } else {
            MessageOutput.lnprint("Field access encountered", field.toString());
        }
    }

    @Override
    public void stepEvent(StepEvent se)  {
        Thread.yield();  // fetch output
        MessageOutput.lnprint("Step completed:");
    }

    @Override
    public void exceptionEvent(ExceptionEvent ee) {
        Thread.yield();  // fetch output
        Location catchLocation = ee.catchLocation();
        if (catchLocation == null) {
            MessageOutput.lnprint("Exception occurred uncaught",
                                  ee.exception().referenceType().name());
        } else {
            MessageOutput.lnprint("Exception occurred caught",
                                  new Object [] {ee.exception().referenceType().name(),
                                                 Commands.locationString(catchLocation)});
        }
    }

    @Override
    public void methodEntryEvent(MethodEntryEvent me) {
        Thread.yield();  // fetch output
        /*
         * These can be very numerous, so be as efficient as possible.
         * If we are stopping here, then we will see the normal location
         * info printed.
         */
        if (me.request().suspendPolicy() != EventRequest.SUSPEND_NONE) {
            // We are stopping; the name will be shown by the normal mechanism
            MessageOutput.lnprint("Method entered:");
        } else {
            // We aren't stopping, show the name
            MessageOutput.print("Method entered:");
            printLocationOfEvent(me);
        }
    }

    @Override
    public boolean methodExitEvent(MethodExitEvent me) {
        Thread.yield();  // fetch output
        /*
         * These can be very numerous, so be as efficient as possible.
         */
        Method mmm = Env.atExitMethod();
        Method meMethod = me.method();

        if (mmm == null || mmm.equals(meMethod)) {
            // Either we are not tracing a specific method, or we are
            // and we are exitting that method.

            if (me.request().suspendPolicy() != EventRequest.SUSPEND_NONE) {
                // We will be stopping here, so do a newline
                MessageOutput.println();
            }
            if (Env.vm().canGetMethodReturnValues()) {
                MessageOutput.print("Method exitedValue:", me.returnValue() + "");
            } else {
                MessageOutput.print("Method exited:");
            }

            if (me.request().suspendPolicy() == EventRequest.SUSPEND_NONE) {
                // We won't be stopping here, so show the method name
                printLocationOfEvent(me);

            }

            // In case we want to have a one shot trace exit some day, this
            // code disables the request so we don't hit it again.
            if (false) {
                // This is a one shot deal; we don't want to stop
                // here the next time.
                Env.setAtExitMethod(null);
                EventRequestManager erm = Env.vm().eventRequestManager();
                for (EventRequest eReq : erm.methodExitRequests()) {
                    if (eReq.equals(me.request())) {
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
    public void vmInterrupted() {
        Thread.yield();  // fetch output
        printCurrentLocation();
        // for (String cmd : monitorCommands) {
        //     StringTokenizer t = new StringTokenizer(cmd);
        //     t.nextToken();  // get rid of monitor number
        //     executeCommand(t);
        // }
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

    private void printLocationOfEvent(LocatableEvent theEvent) {
        printBaseLocation(theEvent.thread().name(), theEvent.location());
    }

    // void help() {
    //     MessageOutput.println("zz help text");
    // }

    // private static final String[][] commandList = {
    //     /*
    //      * NOTE: this list must be kept sorted in ascending ASCII
    //      *       order by element [0].  Ref: isCommand() below.
    //      *
    //      *Command      OK when        OK when
    //      * name      disconnected?   readonly?
    //      *------------------------------------
    //      */
    //     {"!!",           "n",         "y"},
    //     {"?",            "y",         "y"},
    //     {"bytecodes",    "n",         "y"},
    //     {"catch",        "y",         "n"},
    //     {"class",        "n",         "y"},
    //     {"classes",      "n",         "y"},
    //     {"classpath",    "n",         "y"},
    //     {"clear",        "y",         "n"},
    //     {"connectors",   "y",         "y"},
    //     {"cont",         "n",         "n"},
    //     {"disablegc",    "n",         "n"},
    //     {"down",         "n",         "y"},
    //     {"dump",         "n",         "y"},
    //     {"enablegc",     "n",         "n"},
    //     {"eval",         "n",         "y"},
    //     {"exclude",      "y",         "n"},
    //     {"exit",         "y",         "y"},
    //     {"extension",    "n",         "y"},
    //     {"fields",       "n",         "y"},
    //     {"gc",           "n",         "n"},
    //     {"help",         "y",         "y"},
    //     {"ignore",       "y",         "n"},
    //     {"interrupt",    "n",         "n"},
    //     {"kill",         "n",         "n"},
    //     {"lines",        "n",         "y"},
    //     {"list",         "n",         "y"},
    //     {"load",         "n",         "y"},
    //     {"locals",       "n",         "y"},
    //     {"lock",         "n",         "n"},
    //     {"memory",       "n",         "y"},
    //     {"methods",      "n",         "y"},
    //     {"monitor",      "n",         "n"},
    //     {"next",         "n",         "n"},
    //     {"pop",          "n",         "n"},
    //     {"print",        "n",         "y"},
    //     {"quit",         "y",         "y"},
    //     {"read",         "y",         "y"},
    //     {"redefine",     "n",         "n"},
    //     {"reenter",      "n",         "n"},
    //     {"resume",       "n",         "n"},
    //     {"run",          "y",         "n"},
    //     {"save",         "n",         "n"},
    //     {"set",          "n",         "n"},
    //     {"sleep",        "y",         "y"},
    //     {"sourcepath",   "y",         "y"},
    //     {"step",         "n",         "n"},
    //     {"stepi",        "n",         "n"},
    //     {"stop",         "y",         "n"},
    //     {"suspend",      "n",         "n"},
    //     {"thread",       "n",         "y"},
    //     {"threadgroup",  "n",         "y"},
    //     {"threadgroups", "n",         "y"},
    //     {"threadlocks",  "n",         "y"},
    //     {"threads",      "n",         "y"},
    //     {"trace",        "n",         "n"},
    //     {"unmonitor",    "n",         "n"},
    //     {"untrace",      "n",         "n"},
    //     {"unwatch",      "y",         "n"},
    //     {"up",           "n",         "y"},
    //     {"use",          "y",         "y"},
    //     {"version",      "y",         "y"},
    //     {"watch",        "y",         "n"},
    //     {"where",        "n",         "y"},
    //     {"wherei",       "n",         "y"},
    // };

    // /*
    //  * Look up the command string in commandList.
    //  * If found, return the index.
    //  * If not found, return index < 0
    //  */
    // private int isCommand(String key) {
    //     //Reference: binarySearch() in java/util/Arrays.java
    //     //           Adapted for use with String[][0].
    //     int low = 0;
    //     int high = commandList.length - 1;
    //     while (low <= high) {
    //         int mid = (low + high) >>> 1;
    //         String midVal = commandList[mid][0];
    //         int compare = midVal.compareTo(key);
    //         if (compare < 0) {
    //             low = mid + 1;
    //         } else if (compare > 0) {
    //             high = mid - 1;
    //         }
    //         else {
    //             return mid; // key found
    //     }
    //     }
    //     return -(low + 1);  // key not found.
    // };

    // /*
    //  * Return true if the command is OK when disconnected.
    //  */
    // private boolean isDisconnectCmd(int ii) {
    //     if (ii < 0 || ii >= commandList.length) {
    //         return false;
    //     }
    //     return (commandList[ii][1].equals("y"));
    // }

    // /*
    //  * Return true if the command is OK when readonly.
    //  */
    // private boolean isReadOnlyCmd(int ii) {
    //     if (ii < 0 || ii >= commandList.length) {
    //         return false;
    //     }
    //     return (commandList[ii][2].equals("y"));
    // };


    // void executeCommand(StringTokenizer t) {
    //     String cmd = t.nextToken().toLowerCase();
    //     // Normally, prompt for the next command after this one is done
    //     boolean showPrompt = true;


    //     /*
    //      * Anything starting with # is discarded as a no-op or 'comment'.
    //      */
    //     if (!cmd.startsWith("#")) {
    //         /*
    //          * Next check for an integer repetition prefix.  If found,
    //          * recursively execute cmd that number of times.
    //          */
    //         if (Character.isDigit(cmd.charAt(0)) && t.hasMoreTokens()) {
    //             try {
    //                 int repeat = Integer.parseInt(cmd);
    //                 String subcom = t.nextToken("");
    //                 while (repeat-- > 0) {
    //                     executeCommand(new StringTokenizer(subcom));
    //                     showPrompt = false; // Bypass the printPrompt() below.
    //                 }
    //             } catch (NumberFormatException exc) {
    //                 MessageOutput.println("Unrecognized command.  Try help...", cmd);
    //             }
    //         } else {
    //             int commandNumber = isCommand(cmd);
    //             /*
    //              * Check for an unknown command
    //              */
    //             if (commandNumber < 0) {
    //                 MessageOutput.println("Unrecognized command.  Try help...", cmd);
    //             } else if (!Env.connection().isOpen() && !isDisconnectCmd(commandNumber)) {
    //                 MessageOutput.println("Command not valid until the VM is started with the run command",
    //                                       cmd);
    //             } else if (Env.connection().isOpen() && !Env.vm().canBeModified() &&
    //                        !isReadOnlyCmd(commandNumber)) {
    //                 MessageOutput.println("Command is not supported on a read-only VM connection",
    //                                       cmd);
    //             } else {

    //                 Commands evaluator = new Commands();
    //                 try {
    //                     if (cmd.equals("print")) {
    //                         evaluator.commandPrint(t, false);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("eval")) {
    //                         evaluator.commandPrint(t, false);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("set")) {
    //                         evaluator.commandSet(t);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("dump")) {
    //                         evaluator.commandPrint(t, true);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("locals")) {
    //                         evaluator.commandLocals();
    //                     } else if (cmd.equals("classes")) {
    //                         evaluator.commandClasses();
    //                     } else if (cmd.equals("class")) {
    //                         evaluator.commandClass(t);
    //                     } else if (cmd.equals("connectors")) {
    //                         evaluator.commandConnectors(Bootstrap.virtualMachineManager());
    //                     } else if (cmd.equals("methods")) {
    //                         evaluator.commandMethods(t);
    //                     } else if (cmd.equals("fields")) {
    //                         evaluator.commandFields(t);
    //                     } else if (cmd.equals("threads")) {
    //                         evaluator.commandThreads(t);
    //                     } else if (cmd.equals("thread")) {
    //                         evaluator.commandThread(t);
    //                     } else if (cmd.equals("suspend")) {
    //                         evaluator.commandSuspend(t);
    //                     } else if (cmd.equals("resume")) {
    //                         evaluator.commandResume(t);
    //                     } else if (cmd.equals("cont")) {
    //                         evaluator.commandCont();
    //                     } else if (cmd.equals("threadgroups")) {
    //                         evaluator.commandThreadGroups();
    //                     } else if (cmd.equals("threadgroup")) {
    //                         evaluator.commandThreadGroup(t);
    //                     } else if (cmd.equals("catch")) {
    //                         evaluator.commandCatchException(t);
    //                     } else if (cmd.equals("ignore")) {
    //                         evaluator.commandIgnoreException(t);
    //                     } else if (cmd.equals("step")) {
    //                         evaluator.commandStep(t);
    //                     } else if (cmd.equals("stepi")) {
    //                         evaluator.commandStepi();
    //                     } else if (cmd.equals("next")) {
    //                         evaluator.commandNext();
    //                     } else if (cmd.equals("kill")) {
    //                         evaluator.commandKill(t);
    //                     } else if (cmd.equals("interrupt")) {
    //                         evaluator.commandInterrupt(t);
    //                     } else if (cmd.equals("trace")) {
    //                         evaluator.commandTrace(t);
    //                     } else if (cmd.equals("untrace")) {
    //                         evaluator.commandUntrace(t);
    //                     } else if (cmd.equals("where")) {
    //                         evaluator.commandWhere(t, false);
    //                     } else if (cmd.equals("wherei")) {
    //                         evaluator.commandWhere(t, true);
    //                     } else if (cmd.equals("up")) {
    //                         evaluator.commandUp(t);
    //                     } else if (cmd.equals("down")) {
    //                         evaluator.commandDown(t);
    //                     } else if (cmd.equals("load")) {
    //                         evaluator.commandLoad(t);
    //                     } else if (cmd.equals("run")) {
    //                         evaluator.commandRun(t);
    //                         /*
    //                          * Fire up an event handler, if the connection was just
    //                          * opened. Since this was done from the run command
    //                          * we don't stop the VM on its VM start event (so
    //                          * arg 2 is false).
    //                          */
    //                         if ((handler == null) && Env.connection().isOpen()) {
    //                             handler = new EventHandler(this, false);
    //                         }
    //                     } else if (cmd.equals("memory")) {
    //                         evaluator.commandMemory();
    //                     } else if (cmd.equals("gc")) {
    //                         evaluator.commandGC();
    //                     } else if (cmd.equals("stop")) {
    //                         evaluator.commandStop(t);
    //                     } else if (cmd.equals("clear")) {
    //                         evaluator.commandClear(t);
    //                     } else if (cmd.equals("watch")) {
    //                         evaluator.commandWatch(t);
    //                     } else if (cmd.equals("unwatch")) {
    //                         evaluator.commandUnwatch(t);
    //                     } else if (cmd.equals("list")) {
    //                         evaluator.commandList(t);
    //                     } else if (cmd.equals("lines")) { // Undocumented command: useful for testing.
    //                         evaluator.commandLines(t);
    //                     } else if (cmd.equals("classpath")) {
    //                         evaluator.commandClasspath(t);
    //                     } else if (cmd.equals("use") || cmd.equals("sourcepath")) {
    //                         evaluator.commandUse(t);
    //                     } else if (cmd.equals("monitor")) {
    //                         monitorCommand(t);
    //                     } else if (cmd.equals("unmonitor")) {
    //                         unmonitorCommand(t);
    //                     } else if (cmd.equals("lock")) {
    //                         evaluator.commandLock(t);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("threadlocks")) {
    //                         evaluator.commandThreadlocks(t);
    //                     } else if (cmd.equals("disablegc")) {
    //                         evaluator.commandDisableGC(t);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("enablegc")) {
    //                         evaluator.commandEnableGC(t);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("save")) { // Undocumented command: useful for testing.
    //                         evaluator.commandSave(t);
    //                         showPrompt = false;        // asynchronous command
    //                     } else if (cmd.equals("bytecodes")) { // Undocumented command: useful for testing.
    //                         evaluator.commandBytecodes(t);
    //                     } else if (cmd.equals("redefine")) {
    //                         evaluator.commandRedefine(t);
    //                     } else if (cmd.equals("pop")) {
    //                         evaluator.commandPopFrames(t, false);
    //                     } else if (cmd.equals("reenter")) {
    //                         evaluator.commandPopFrames(t, true);
    //                     } else if (cmd.equals("extension")) {
    //                         evaluator.commandExtension(t);
    //                     } else if (cmd.equals("exclude")) {
    //                         evaluator.commandExclude(t);
    //                     } else if (cmd.equals("read")) {
    //                         readCommand(t);
    //                     } else if (cmd.equals("sleep")) {
    //                         evaluator.commandSleep(t);
    //                     } else if (cmd.equals("help") || cmd.equals("?")) {
    //                         help();
    //                     } else if (cmd.equals("version")) {
    //                         evaluator.commandVersion(progname,
    //                                                  Bootstrap.virtualMachineManager());
    //                     } else if (cmd.equals("quit") || cmd.equals("exit")) {
    //                         if (handler != null) {
    //                             handler.shutdown();
    //                         }
    //                         Env.shutdown();
    //                     } else {
    //                         MessageOutput.println("Unrecognized command.  Try help...", cmd);
    //                     }
    //                 } catch (VMCannotBeModifiedException rovm) {
    //                     MessageOutput.println("Command is not supported on a read-only VM connection", cmd);
    //                 } catch (UnsupportedOperationException uoe) {
    //                     MessageOutput.println("Command is not supported on the target VM", cmd);
    //                 } catch (VMNotConnectedException vmnse) {
    //                     MessageOutput.println("Command not valid until the VM is started with the run command",
    //                                           cmd);
    //                 } catch (Exception e) {
    //                     MessageOutput.printException("Internal exception:", e);
    //                 }
    //             }
    //         }
    //     }
    //     if (showPrompt) {
    //         MessageOutput.printPrompt();
    //     }
    // }

    /*
     * Maintain a list of commands to execute each time the VM is suspended.
     */
    // void monitorCommand(StringTokenizer t) {
    //     if (t.hasMoreTokens()) {
    //         ++monitorCount;
    //         monitorCommands.add(monitorCount + ": " + t.nextToken(""));
    //     } else {
    //         for (String cmd : monitorCommands) {
    //             MessageOutput.printDirectln(cmd);// Special case: use printDirectln()
    //         }
    //     }
    // }

    // void unmonitorCommand(StringTokenizer t) {
    //     if (t.hasMoreTokens()) {
    //         String monTok = t.nextToken();
    //         int monNum;
    //         try {
    //             monNum = Integer.parseInt(monTok);
    //         } catch (NumberFormatException exc) {
    //             MessageOutput.println("Not a monitor number:", monTok);
    //             return;
    //         }
    //         String monStr = monTok + ":";
    //         for (String cmd : monitorCommands) {
    //             StringTokenizer ct = new StringTokenizer(cmd);
    //             if (ct.nextToken().equals(monStr)) {
    //                 monitorCommands.remove(cmd);
    //                 MessageOutput.println("Unmonitoring", cmd);
    //                 return;
    //             }
    //         }
    //         MessageOutput.println("No monitor numbered:", monTok);
    //     } else {
    //         MessageOutput.println("Usage: unmonitor <monitor#>");
    //     }
    // }

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
        // try {
        //     BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        //     String lastLine = null;

        //     // Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

        //     /*
        //      * Read start up files.  This mimics the behavior
        //      * of gdb which will read both ~/.gdbinit and then
        //      * ./.gdbinit if they exist.  We have the twist that
        //      * we allow two different names, so we do this:
        //      *  if ~/jdb.ini exists,
        //      *      read it
        //      *  else if ~/.jdbrc exists,
        //      *      read it
        //      *
        //      *  if ./jdb.ini exists,
        //      *      if it hasn't been read, read it
        //      *      It could have been read above because ~ == .
        //      *      or because of symlinks, ...
        //      *  else if ./jdbrx exists
        //      *      if it hasn't been read, read it
        //      */
        //     {
        //         String userHome = System.getProperty("user.home");
        //         String canonPath;

        //         if ((canonPath = readStartupCommandFile(userHome, "jdb.ini", null)) == null) {
        //             // Doesn't exist, try alternate spelling
        //             canonPath = readStartupCommandFile(userHome, ".jdbrc", null);
        //         }

        //         String userDir = System.getProperty("user.dir");
        //         if (readStartupCommandFile(userDir, "jdb.ini", canonPath) == null) {
        //             // Doesn't exist, try alternate spelling
        //             readStartupCommandFile(userDir, ".jdbrc", canonPath);
        //         }
        //     }

        //     // Process interactive commands.
        //     MessageOutput.printPrompt();
        //     while (true) {
        //         String ln = in.readLine();
        //         if (ln == null) {
        //             /*
        //              *  Jdb is being shutdown because debuggee exited, ignore any 'null'
        //              *  returned by readLine() during shutdown. JDK-8154144.
        //              */
        //             if (!isShuttingDown()) {
        //                 MessageOutput.println("Input stream closed.");
        //             }
        //             ln = "quit";
        //         }

        //         if (ln.startsWith("!!") && lastLine != null) {
        //             ln = lastLine + ln.substring(2);
        //             MessageOutput.printDirectln(ln);// Special case: use printDirectln()
        //         }

        //         StringTokenizer t = new StringTokenizer(ln);
        //         if (t.hasMoreTokens()) {
        //             lastLine = ln;
        //             executeCommand(t);
        //         } else {
        //             MessageOutput.printPrompt();
        //         }
        //     }
        // } catch (VMDisconnectedException e) {
        //     handler.handleDisconnectedException();
        // }
    }
}
