package com.bear27570.yuan.BotFactory.Gamepad;

import com.qualcomm.robotcore.util.ElapsedTime;

public class GamepadValue {
    private double nowPosition,lastPosition;
    private ElapsedTime PressTimer = new ElapsedTime();
    private GamepadValue (){
        nowPosition = 0;
        lastPosition = 0;
        PressTimer.reset();
    }
    public static GamepadValue initValue(){
        return new GamepadValue();
    }
    public void update(double currentValue){
        this.lastPosition = this.nowPosition;
        this.nowPosition = currentValue;
        PressTimer.reset();
    }
    public double PressVelocity(){
        double c = nowPosition-lastPosition;
        return c/PressTimer.milliseconds();
    }
    public double PressPosition(){
        return nowPosition;
    }
    public boolean justPressed(){
        return nowPosition!=0&&lastPosition==0;
    }
    public boolean justReleased(){
        return nowPosition==0&&lastPosition!=0;
    }
}
