package eu.weischer.root.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import eu.weischer.root.application.App;

public class MainExecutorService implements ExecutorService {
    public static MainExecutorService getMainExecutorService() {
        if (theMainExecutorService == null)
            theMainExecutorService = new MainExecutorService();
        return theMainExecutorService;
    }
    private static MainExecutorService theMainExecutorService = null;
    private Executor executor = null;
    private MainExecutorService() {
        executor = App.getContext().getMainExecutor();
    }
    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }
    @Override
    public void shutdown() {
    }
    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }
    @Override
    public boolean isShutdown() {
        return false;
    }
    @Override
    public boolean isTerminated() {
        return false;
    }
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<T>(task);
        executor.execute(futureTask);
        return futureTask;
    }
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        FutureTask<T> futureTask = new FutureTask<T>(task, result);
        executor.execute(futureTask);
        return futureTask;
    }
    @Override
    public Future<?> submit(Runnable task) {
        FutureTask<?> futureTask = new FutureTask<Void>(task, null);
        executor.execute(futureTask);
        return futureTask;
    }
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return tasks.stream().map(this::submit).collect(Collectors.toList());
    }
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return tasks.stream().map(this::submit).collect(Collectors.toList());
    }
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return tasks.stream().map(this::submit).findFirst().get().get();
    }
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return tasks.stream().map(this::submit).findFirst().get().get();
    }
}
