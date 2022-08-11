package fr.aym.acslib.services.impl.thrload;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ErrorManagerService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.utils.ACsLogger;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoaderException;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DThreadedLoadingTask implements Runnable {
    private final Runnable task;
    private final ThreadedLoadingService.ModLoadingSteps endBefore;
    private final int id;
    private final String name;
    private final Runnable followingInThreadTask;
    private final DynamXThreadedModLoader executor;

    private static int lastId;

    public DThreadedLoadingTask(Runnable task, ThreadedLoadingService.ModLoadingSteps endBefore, Runnable followingInThreadTask, String taskName, DynamXThreadedModLoader executor) {
        this.task = task;
        this.endBefore = endBefore;
        this.followingInThreadTask = followingInThreadTask;
        this.executor = executor;
        this.name = taskName;
        id = lastId;
        lastId++;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        try {
            task.run();
            executor.onEnd(this, followingInThreadTask, (System.currentTimeMillis() - time));
        } catch (Exception e) {
            ACsLogger.serviceFatal(ACsLib.getPlatform().provideService(ThreadedLoadingService.class), "Error in task " + name, e);
            DynamXErrorManager.addError("DynamX initialization", "loading_tasks", ErrorManagerService.ErrorLevel.FATAL, "ThreadTask " + name, null, e, 1000);
            if (FMLCommonHandler.instance().getSide().isClient()) {
                executor.onEnd(this, () -> {
                    if (e instanceof CustomModLoadingErrorDisplayException)
                        throw e;
                    throw new ThreadedLoadingException(name, e);
                }, (System.currentTimeMillis() - time));
            } else {
                executor.onEnd(this, () -> {
                    throw new ThreadedLoadingException(name, e);
                }, (System.currentTimeMillis() - time));
            }
        }
    }

    public boolean shouldEndNow(ThreadedLoadingService.ModLoadingSteps step) {
        return endBefore.getIndex() <= step.getIndex();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ThreadedLoadingTask{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public static class ThreadedLoadingException extends LoaderException {
        public ThreadedLoadingException(String taskName, Exception e) {
            super("Exception in task " + taskName, e);
            setStackTrace(new StackTraceElement[0]);
        }

        @Override
        public String toString() {
            return getLocalizedMessage();
        }

        @Override
        public void printStackTrace(final PrintWriter s) {
            super.printStackTrace(s);
            printCustomMessage(new WrappedPrintStream() {
                @Override
                public void println(String line) {
                    s.println(line);
                }
            });
        }

        @Override
        public void printStackTrace(final PrintStream s) {
            super.printStackTrace(s);
            printCustomMessage(new WrappedPrintStream() {
                @Override
                public void println(String line) {
                    s.println(line);
                }
            });
        }

        protected void printCustomMessage(WrappedPrintStream stream) {
        }
    }
}
