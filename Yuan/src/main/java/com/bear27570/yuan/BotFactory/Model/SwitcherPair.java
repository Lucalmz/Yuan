package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;

/**
 * 两个动作的共同封装类，用于switch方法
 */
public class SwitcherPair {
    public Action getSwitch1() {
        return switch1;
    }

    private final Action switch1;

    public Action getSwitch2() {
        return switch2;
    }

    private final Action switch2;
    private SwitcherPair(Action s1,Action s2){
        switch1 = s1;
        switch2 = s2;
    }
    public static SwitcherPair GetSwitcherPair(Action s1,Action s2){
        return new SwitcherPair(s1,s2);
    }
}
