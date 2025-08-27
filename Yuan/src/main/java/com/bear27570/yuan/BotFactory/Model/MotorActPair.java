package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Motor.MotorEx;
/**
 * DC电机和动作的共同封装类，用于safety check
 */
public class MotorActPair {
    private final MotorEx Motor;
    private final Action thisAct;
    public MotorActPair(MotorEx Motor, Action thisAct){
        this.Motor = Motor;
        this.thisAct = thisAct;
    }
    public void run(){
        Motor.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public MotorEx getMotor(){
        return Motor;
    }
}
