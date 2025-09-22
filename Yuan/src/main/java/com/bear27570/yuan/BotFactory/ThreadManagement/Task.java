package com.bear27570.yuan.BotFactory.ThreadManagement;

import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.bear27570.yuan.BotFactory.Model.ConflictPolicy;
import com.bear27570.yuan.BotFactory.Model.Priority;

import java.util.HashSet;
import java.util.Set;

public class Task implements Comparable<Task>{
    private Runnable mainTask;
    private Runnable inInterruptCleanUp;
    private Set<Lockable> requirements = new HashSet<>();
    private final Priority priority;
    private final ConflictPolicy policy;

    public Runnable getMainTask() {
        return mainTask;
    }

    public Runnable getInInterruptCleanUp() {
        return inInterruptCleanUp;
    }

    public Set<Lockable> getRequirements() {
        return requirements;
    }

    public Priority getPriority() {
        return priority;
    }

    public ConflictPolicy getPolicy() {
        return policy;
    }
    @Override
    public int compareTo(Task o) {
        return this.priority.compareTo(o.priority);
    }
    private Task(TaskBuilder builder){
        this.mainTask = builder.mainTask;
        this.inInterruptCleanUp = builder.inInterruptCleanUp;
        this.requirements = builder.requirements;
        this.priority = builder.priority;
        this.policy = builder.policy;
    }
    public static class TaskBuilder {
        private Runnable mainTask;
        private Runnable inInterruptCleanUp = () -> {};
        private Set<Lockable> requirements = new HashSet<>();
        private final Priority priority;
        private final ConflictPolicy policy;
        public TaskBuilder(Priority priority, ConflictPolicy policy) {
            this.priority = priority;
            this.policy = policy;
        }
        public TaskBuilder runs(Runnable mainTask) {
            this.mainTask = mainTask;
            return this;
        }
        public TaskBuilder onInterrupt(Runnable inInterruptCleanUp) {
            this.inInterruptCleanUp = inInterruptCleanUp;
            return this;
        }
        public TaskBuilder require(Lockable requirement) {
            this.requirements.add(requirement);
            return this;
        }
        public Task build() {
            return new Task(this);
        }
    }
}