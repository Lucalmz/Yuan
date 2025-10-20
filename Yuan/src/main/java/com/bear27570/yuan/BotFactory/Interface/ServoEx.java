package com.bear27570.yuan.BotFactory.Interface;

import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.ThreadManagement.Task;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

public interface ServoEx {
    public PriorityBlockingQueue<Task> getWaitingQueue();
    public boolean tryLock();
    public void lock();
    public void unlock();
    public void act(Action action);
    public Action getState();
    public String getConfig(int i);
    public void Switch();
    public void Init();
    public boolean whichIsReversed(int i);
    public void shutdownVelThread();
    public void SetTemporaryPosition(double TemporaryPosition);
    public void BlockedActWithVel(double DegPerSec);
    public void StopVelTurning();
    public void periodic();
    public void PatientAct(Action thisAction) throws InterruptedException;
    public long WaitMillSec();
    public double getActionPosition(Action target);
    public double getServoMaxVel();
    public int getDegRange();
    public void actWithVel(double DegPerSec);
    public double getVelocity();
    public void setVelocity(double degreesPerSecond);
    public HashMap<Action, Double> getNameList();

}
