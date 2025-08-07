package com.bear27570.yuan.BotFactory;

public interface RunnableStructUnit {
    void act(Action action);
    void Init();
    Action getState();
    String getConfig(int i);
    boolean whichIsReversed(int i);
    void Switch();
}
