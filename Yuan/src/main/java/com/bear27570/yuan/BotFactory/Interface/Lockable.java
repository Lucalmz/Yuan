package com.bear27570.yuan.BotFactory.Interface;

import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.ThreadManagement.Task;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public interface Lockable{
    /**
     * 全部上锁方法
     */
    public boolean tryLock();
    public void lock();
    /**
     * 全部释放锁方法
     */
    public void unlock();
    public void act(Action action);
    public PriorityBlockingQueue<Task> getWaitingQueue();

}
