package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Servo.ServoEx;
/**
 * 舵机和动作的共同封装类，用于safety check
 */
public class ServoActPair {
    private final ServoEx servo;
    private final Action thisAct;
    public ServoActPair(ServoEx servo, Action thisAct){
        this.servo = servo;
        this.thisAct = thisAct;
    }
    public void run(){
        servo.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public ServoEx getServo(){
        return servo;
    }
}
