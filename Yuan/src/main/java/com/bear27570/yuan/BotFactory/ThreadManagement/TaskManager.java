package com.bear27570.yuan.BotFactory.ThreadManagement;

import com.bear27570.yuan.BotFactory.Interface.Lockable;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TaskManager {
    private final ExecutorService executor = new ThreadPoolExecutor(
            0,Integer.MAX_VALUE,
            30L, TimeUnit.SECONDS,
            new SynchronousQueue<>());
    private final ConcurrentMap<Lockable, RunningTaskInfo> runningTasks = new ConcurrentHashMap<>();
    private final ReentrantLock SchedulerLock = new ReentrantLock();
    public void submit(Task task){
        SchedulerLock.lock();
        try {
            RunningTaskInfo conflictTask = findConflictTask(task.requirements);
            if (conflictTask == null) {
                executeTask(task);
                return;
            }
            //出现冲突时
            switch (task.getPolicy()) {
                case IGNORE:
                    break;
                case QUEUE:
                    findConflictSubsystem(task.requirements).getWaitingQueue().add(task);
                    break;
                case INTERRUPT:
                    //优先权判断，避免低优先打断高优先程序
                    if(conflictTask.task.getPriority().ordinal() > task.getPriority().ordinal()){
                        break;
                    }
                    clearQueuesFor(conflictTask);
                    conflictTask.future.cancel(true);
                    executeTask(task);
                    break;
            }
        }finally {
            SchedulerLock.unlock();
        }
    }
    private void executeTask(Task task) {
        Runnable taskWrapper = () -> {
            try {
                task.getRequirements().forEach(Lockable::lock);
                task.getMainTask().run();
            } catch (Exception e) {
                // 捕获所有异常，包括潜在的InterruptedException被包装的情况
                if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                    task.getInInterruptCleanUp().run();
                    Thread.currentThread().interrupt();
                } else {
                    System.out.println("  [THREAD] 💥任务执行时发生未知错误: " + e.getMessage());
                }
            } finally {
                task.getRequirements().forEach(Lockable::unlock);
                onTaskFinished(task);
            }
        };
        Future<?> future = executor.submit(taskWrapper);
        RunningTaskInfo info = new RunningTaskInfo(task, future);
        task.getRequirements().forEach(s -> runningTasks.put(s, info));
    }
    private Lockable findConflictSubsystem(Set<Lockable> required) {
        for (Lockable unit : required) {
            if (runningTasks.containsKey(unit)) {
                return unit;
            }
        }
        return null;
    }
    private RunningTaskInfo findConflictTask(Set<Lockable> required){
        for (Lockable unit : required) {
            if (runningTasks.containsKey(unit)) {
                return runningTasks.get(unit);
            }
        }
        return null;
    }
    private void onTaskFinished(Task finishedTask) {
        SchedulerLock.lock();
        try {
            finishedTask.getRequirements().forEach(s -> runningTasks.remove(s, findRunningTaskInfoFor(s, finishedTask)));
            // 检查刚刚被释放的资源的队列，启动新任务
            for (Lockable subsystem : finishedTask.getRequirements()) {
                if (!subsystem.getWaitingQueue().isEmpty()) {
                    Task nextTask = subsystem.getWaitingQueue().peek();
                    if (areAllRequirementsMet(nextTask)) {
                        // 提交下一个任务，这会自动检查冲突（理论上此时不应有冲突）
                        subsystem.getWaitingQueue().poll();
                        submit(nextTask);
                        break;
                    }
                }
            }
        }finally {
            SchedulerLock.unlock();
        }
    }
    private RunningTaskInfo findRunningTaskInfoFor(Lockable Unit, Task t) {
        RunningTaskInfo info = runningTasks.get(Unit);
        if (info != null && info.task.equals(t)) {
            return info;
        }
        return null; // Should not happen in synchronized context
    }

    private void clearQueuesFor(RunningTaskInfo taskToClear) {
        for (Lockable subsystem : taskToClear.task.getRequirements()) {
            if (!subsystem.getWaitingQueue().isEmpty()) {
                // 清空该子系统的任务队列
                subsystem.getWaitingQueue().clear();
            }
        }
    }
    private boolean areAllRequirementsMet(Task task) {
        for (Lockable required : task.getRequirements()) {
            if (runningTasks.containsKey(required)) {
                // 只要有一个所需资源还在被占用，就不能运行
                return false;
            }
        }
        return true;
    }
}