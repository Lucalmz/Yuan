package com.bear27570.yuan.BotFactory;

import com.bear27570.yuan.BotFactory.Servo.CRServoFactory;
import com.bear27570.yuan.BotFactory.Servo.ServoFactory;

public class CRServoActPair {
    private final CRServoFactory servo;
    private final Action thisAct;
    public CRServoActPair(CRServoFactory servo,Action thisAct){
        this.servo = servo;
        this.thisAct = thisAct;
    }
    public void run(){
        servo.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public CRServoFactory getServo(){
        return servo;
    }
}
