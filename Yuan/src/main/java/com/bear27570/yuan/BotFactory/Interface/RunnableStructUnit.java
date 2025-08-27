package com.bear27570.yuan.BotFactory.Interface;

import com.bear27570.yuan.BotFactory.Action;

import java.util.HashMap;

/**
 * RunnableStructUnit 单元自由度的接口，实现公用功能
 */
public interface RunnableStructUnit {
    void lock();
    void unlock();
    void act(Action action);
    void PatientAct(Action action) throws InterruptedException;
    void Init();
    Action getState();
    String getConfig(int i);
    HashMap<Action,Double> getNameList();
    boolean whichIsReversed(int i);
    void Switch();
}
