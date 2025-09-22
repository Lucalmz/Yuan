package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Interface.Lockable;

import java.util.ArrayList;

/**
 * 实现Lockable的结构单元和动作的共同封装类，用于safety check
 */
public class LockableActPair {
    private ArrayList<Lockable> unit = new ArrayList<>();
    private final Action thisAct;

    //构造Pair对象
    public LockableActPair(Action act) {
        thisAct = act;
    }

    public void addUnit(Lockable unit) {
        this.unit.add(unit);
    }

    //SafetyCheck直接run
    public void run() {
        for (int i = 0; i < unit.size(); i++)
            unit.get(i).act(thisAct);
    }

    public Action getRelevantAction() {
        return thisAct;
    }

    public ArrayList<Lockable> getUnitList() {
        return unit;
    }
}
