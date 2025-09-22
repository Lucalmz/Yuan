package com.bear27570.yuan.BotFactory.ThreadManagement;

import com.bear27570.yuan.BotFactory.Interface.Lockable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

class RunningTaskInfo {
    final Task task;
    final Future<?> future;

    RunningTaskInfo(Task task, Future<?> future) {
        this.task = task;
        this.future = future;
    }
}
