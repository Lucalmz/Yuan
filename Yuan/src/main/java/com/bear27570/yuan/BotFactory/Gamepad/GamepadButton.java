package com.bear27570.yuan.BotFactory.Gamepad;

import com.qualcomm.robotcore.util.ElapsedTime;


/**
 * 增强的Gamepad按键，支持边缘及长按检测
 * 支持同步线程，附加了一个对应的线程
 */
public class GamepadButton {
    private ElapsedTime ButtonTimer = new ElapsedTime();
    private boolean nowPressed,previousPressed;
    //创建空线程，在客户端被覆盖
    public Thread thread = new Thread(()->{});

    /**
     * 私有构造函数，从静态工厂创建实例
     */
    private GamepadButton(){
        nowPressed = false;
        previousPressed = false;
        ButtonTimer.reset();
    }

    /**
     * 静态工厂方法创建实例
     * @return 一个新的GamepadButton类型实例
     */
    protected static GamepadButton initButton(){
        return new GamepadButton();
    }

    /**
     * 更新该按键
     * @param CurrentState 当前Gamepad对应的状态
     */
    protected void update(boolean CurrentState){
        this.previousPressed = this.nowPressed;
        this.nowPressed = CurrentState;
        thread.start();
    }

    /**
     * 刚刚按下按键
     * @return 是否刚按下
     */
    public boolean Pressed(){
        if(nowPressed&&!previousPressed){
            ButtonTimer.reset();
            return true;
        }
        return false;
    }

    /**
     * 正在按下
     * @return 返回是否正在按
     */
    public boolean isPressing(){
        return nowPressed;
    }

    /**
     * 按下时间，可用于长按判定等
     * @return 按下的时间，Unit:ms
     */
    public double PressedTime(){
        return ButtonTimer.milliseconds();
    }

    /**
     * 是否长按
     * @param milliseconds 长按判定条件时间，Unit:ms
     * @return 是否长按
     */
    public boolean LongPressed(double milliseconds){
        if(PressedTime()>milliseconds&&isPressing()){
            return true;
        }
        return false;
    }

    /**
     * 是否刚刚松开
     * @return 是否刚松开
     */
    public boolean justReleased(){
        return previousPressed&&!nowPressed;
    }
}
