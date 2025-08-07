package com.bear27570.yuan.BotFactory;

import com.bear27570.yuan.BotFactory.Motor.MotorFactory;

public class MotorActPair {
    private final MotorFactory Motor;
    private final Action thisAct;
    public MotorActPair(MotorFactory Motor,Action thisAct){
        this.Motor = Motor;
        this.thisAct = thisAct;
    }
    public void run(){
        Motor.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public MotorFactory getMotor(){
        return Motor;
    }
}
