package com.bear27570.yuan.BotFactory;

/**
 * RunnableStructUnit 单元自由度的接口，实现公用功能
 */
public interface RunnableStructUnit {
    void act(Action action);
    void Init();
    Action getState();
    String getConfig(int i);
    boolean whichIsReversed(int i);
    void Switch();
}
