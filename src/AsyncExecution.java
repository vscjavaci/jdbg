package jdbg;

import com.sun.jdi.IncompatibleThreadStateException;

import java.lang.*;
import java.text.*;
import java.util.*;
import java.io.*;

abstract class AsyncExecution {
    abstract void action();

    AsyncExecution() {
        execute();
    }

    void execute() {
        /*
        * Save current thread and stack frame. (BugId 4296031)
        */
        final ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        final int stackFrame = threadInfo == null? 0 : threadInfo.getCurrentFrameIndex();
        Thread thread = new Thread("asynchronous jdb command") {
                @Override
                public void run() {
                    try {
                        action();
                    } catch (UnsupportedOperationException uoe) {
                        //(BugId 4453329)
                        MessageOutput.println("Operation is not supported on the target VM");
                    } catch (Exception e) {
                        MessageOutput.println("Internal exception during operation:",
                                              e.getMessage());
                    } finally {
                        /*
                        * This was an asynchronous command.  Events may have been
                        * processed while it was running.  Restore the thread and
                        * stack frame the user was looking at.  (BugId 4296031)
                        */
                        if (threadInfo != null) {
                            ThreadInfo.setCurrentThreadInfo(threadInfo);
                            try {
                                threadInfo.setCurrentFrameIndex(stackFrame);
                            } catch (IncompatibleThreadStateException e) {
                                MessageOutput.println("Current thread isnt suspended.");
                            } catch (ArrayIndexOutOfBoundsException e) {
                                MessageOutput.println("Requested stack frame is no longer active:",
                                                      new Object []{new Integer(stackFrame)});
                            }
                        }
                        MessageOutput.printPrompt();
                    }
                }
            };
        thread.start();
    }
}
