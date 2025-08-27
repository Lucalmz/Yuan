package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;

public class RSUActPair {
    private final RunnableStructUnit unit;
    private final Action thisAct;
    public RSUActPair(RunnableStructUnit unit, Action thisAct){
        this.unit = unit;
        this.thisAct = thisAct;
    }
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
