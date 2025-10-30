package com.bear27570.yuan.BotFactory.ThreadManagement;

import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TaskManager 负责管理和调度需要独占资源（Lockable）的任务。
 * 它能够处理任务间的资源冲突，并根据预设策略（忽略、排队、中断）来解决这些冲突，
 * 所有调度操作都通过全局锁来确保线程安全。
 */
@ThreadSafe
public class TaskManager {
    private static TaskManager INSTANCE;
    // 使用缓存线程池来执行任务，适合处理大量、短暂的突发性任务。
    private final ExecutorService executor;

    // 存储当前正在运行的任务，Key为被占用的资源，Value为任务信息。
    private final ConcurrentMap<Lockable, RunningTaskInfo> runningTasks;
    private final PriorityBlockingQueue<Task> waitingTasks = new PriorityBlockingQueue<>();

    // 用于保护任务调度逻辑的全局锁。
    private final ReentrantLock SchedulerLock;
    private TaskManager() {
        this.executor = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                30L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.runningTasks = new ConcurrentHashMap<>();
        this.SchedulerLock = new ReentrantLock();
    }
    public static synchronized TaskManager getInstance(){
        if(INSTANCE==null){
            INSTANCE = new TaskManager();
        }
        return INSTANCE;
    }
    /**
     * 提交一个新任务进行调度。
     * 此方法会检查任务的资源需求，如果存在冲突，则根据任务的冲突策略进行处理。
     * @param task 要提交的任务。
     */
    public void submit(Task task){
        SchedulerLock.lock();
        try {
            RunningTaskInfo conflictTask = findConflictTask(task.getRequirements());
            if (conflictTask == null) {
                executeTask(task);
                return;
            }

            // 出现资源冲突时的处理
            switch (task.getPolicy()) {
                case IGNORE:
                    break;
                case QUEUE:
                    waitingTasks.add(task);
                    break;
                case INTERRUPT:
                    // 避免低优先级任务中断高优先级任务（枚举的ordinal值越小，优先级越高）
                    if(conflictTask.task.getPriority().ordinal() > task.getPriority().ordinal()){
                        break;
                    }
                    conflictTask.future.cancel(true);
                    executeTask(task);
                    break;
            }
        }finally {
            SchedulerLock.unlock();
        }
    }

    /**
     * 内部方法，负责将任务包装后提交到线程池执行。
     * 它处理资源的锁定、任务执行、异常捕获、资源释放和任务完成后的回调。
     * @param task 要执行的任务。
     */
    private void executeTask(Task task) {
        Runnable taskWrapper = () -> {
            try {
                task.getRequirements().forEach(Lockable::lock);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task was interrupted while acquiring locks.");
                }
                task.getMainTask().run();
            } catch (InterruptedException e) {
                task.getInInterruptCleanUp().run();
                Thread.currentThread().interrupt();
            } finally {
                task.getRequirements().forEach(Lockable::unlock);
                onTaskFinished(task);
            }
        };
        Future<?> future = executor.submit(taskWrapper);
        RunningTaskInfo info = new RunningTaskInfo(task, future);
        task.getRequirements().forEach(s -> runningTasks.put(s, info));
    }

    /**
     * 查找导致冲突的第一个被占用的资源。
     * @param required 任务所需的资源集合。
     * @return 返回冲突的资源，若无冲突则返回 null。
     */
    private Lockable findConflictSubsystem(Set<Lockable> required) {
        for (Lockable unit : required) {
            if (runningTasks.containsKey(unit)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 查找与指定资源需求相冲突的正在运行的任务。
     * @param required 任务所需的资源集合。
     * @return 返回冲突任务的 {@link RunningTaskInfo}，若无冲突则返回 null。
     */
    private RunningTaskInfo findConflictTask(Set<Lockable> required){
        for (Lockable unit : required) {
            if (runningTasks.containsKey(unit)) {
                return runningTasks.get(unit);
            }
        }
        return null;
    }

    /**
     * 在任务完成后调用，负责清理资源并尝试调度等待队列中的下一个任务。
     * @param finishedTask 已完成的任务。
     */
    private void onTaskFinished(Task finishedTask) {
        SchedulerLock.lock();
        try {
            finishedTask.getRequirements().forEach(s -> runningTasks.remove(s, findRunningTaskInfoFor(s, finishedTask)));
            tryToScheduleWaitingTasks();
        }finally {
            SchedulerLock.unlock();
        }
    }
    /**
     * 遍历整个“等候室”，尝试调度所有现在可以运行的任务。
     */
    private void tryToScheduleWaitingTasks() {
        ArrayList<Task> AvailableTask = new ArrayList<>();

        for (Task waitingTask : waitingTasks) {
            if (areAllRequirementsMet(waitingTask)) {
                AvailableTask.add(waitingTask);
            }
        }
        if (!AvailableTask.isEmpty()) {
            waitingTasks.removeAll(AvailableTask);
            for (Task taskToRun : AvailableTask) {
                executeTask(taskToRun);
            }
        }
    }

    /**
     * 辅助方法，根据资源和任务实例查找对应的 RunningTaskInfo。
     * @param Unit 资源
     * @param t 任务
     * @return 匹配的 RunningTaskInfo，否则返回 null。
     */
    private RunningTaskInfo findRunningTaskInfoFor(Lockable Unit, Task t) {
        RunningTaskInfo info = runningTasks.get(Unit);
        if (info != null && info.task.equals(t)) {
            return info;
        }
        return null;
    }

    /**
     * 检查一个任务所需的所有资源当前是否都可用。
     * @param task 要检查的任务。
     * @return 如果所有资源都可用，返回 true。
     */
    private boolean areAllRequirementsMet(Task task) {
        for (Lockable required : task.getRequirements()) {
            if (runningTasks.containsKey(required)) {
                return false;
            }
        }
        return true;
    }
}