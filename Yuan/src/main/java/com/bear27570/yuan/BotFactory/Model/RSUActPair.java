package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;

/**
 * 实现RSU的结构单元和动作的共同封装类，用于safety check
 */
public class RSUActPair{
    private final RunnableStructUnit unit;
    private final Action thisAct;
    //构造Pair对象
    public RSUActPair(RunnableStructUnit unit, Action thisAct){
        this.unit = unit;
        this.thisAct = thisAct;
    }
    //SafetyCheck直接run
    public void run(){
        unit.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public RunnableStructUnit getUnit(){
        return unit;
    }
}
