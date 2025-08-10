package com.bear27570.yuan.BotFactory.Gamepad;

import com.qualcomm.robotcore.util.ElapsedTime;

class GamepadButton {
    private ElapsedTime ButtonTimer = new ElapsedTime();
    private boolean nowPressed,previousPressed;
    private GamepadButton(){
        nowPressed = false;
        previousPressed = false;
        ButtonTimer.reset();
    }
    protected static GamepadButton initButton(){
        return new GamepadButton();
    }
    public void update(boolean CurrentState){
        this.previousPressed = this.nowPressed;
        this.nowPressed = CurrentState;
    }
    public boolean Pressed(){
        ButtonTimer.reset();
        return nowPressed&&!previousPressed;
    }
    public boolean isPressing(){
        return nowPressed;
    }
    public double PressedTime(){
        return ButtonTimer.milliseconds();
    }
    public boolean LongPressed(double milliseconds){
        if(ButtonTimer.milliseconds()>milliseconds&&isPressing()){
            return true;
        }
        return false;
    }
    public boolean justReleased(){
        return previousPressed&&!nowPressed;
    }
}
