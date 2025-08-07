package com.bear27570.yuan.BotFactory;

import com.bear27570.yuan.BotFactory.Servo.ServoFactory;

public class ServoActPair {
    private final ServoFactory servo;
    private final Action thisAct;
    public ServoActPair(ServoFactory servo,Action thisAct){
        this.servo = servo;
        this.thisAct = thisAct;
    }
    public void run(){
        servo.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public ServoFactory getServo(){
        return servo;
    }
}
