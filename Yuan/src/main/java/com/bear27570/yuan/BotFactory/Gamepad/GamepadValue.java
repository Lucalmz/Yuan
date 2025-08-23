package com.bear27570.yuan.BotFactory.Gamepad;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * 增强版的浮点型gamepad单元，可以支持速度检测和边缘检测
 * v=Δn/Δt(Unit:ms)
 */
public class GamepadValue {
    private double nowPosition,lastPosition;
    public Thread thread;
    private ElapsedTime PressTimer = new ElapsedTime();
    private GamepadValue (){
        nowPosition = 0;
        lastPosition = 0;
        PressTimer.reset();
    }
    protected static GamepadValue initValue(){
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
    public boolean justStartUsed(){
        return nowPosition!=0&&lastPosition==0;
    }
    public boolean Using(){return nowPosition!=0;}
    public boolean justReleased(){
        return nowPosition==0&&lastPosition!=0;
    }
}
