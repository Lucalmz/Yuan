package com.bear27570.yuan.BotFactory.Gamepad;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * 增强版的浮点型gamepad单元，可以支持速度检测和边缘检测
 * v=Δx/Δt(Unit:ms)
 */
public class GamepadValue {
    private double nowPosition,lastPosition;
    private double velocity;
    private ElapsedTime PressTimer = new ElapsedTime();

    /**
     * 私有化构造函数，使用静态工厂创建实例
     */
    private GamepadValue (){
        nowPosition = 0;
        lastPosition = 0;
        PressTimer.reset();
    }

    /**
     * 静态工厂方法创建实例
     * @return 创建新的GamepadValue实例
     */
    protected static GamepadValue initValue(){
        return new GamepadValue();
    }

    /**
     * 更新Gamepad内容
     * @param currentValue 当前的位置数据
     */
    protected void update(double currentValue){
        this.lastPosition = this.nowPosition;
        this.nowPosition = currentValue;
        double deltaTime = PressTimer.milliseconds();
        PressTimer.reset();
        if(deltaTime > 0){
            velocity = (this.nowPosition-this.lastPosition) /deltaTime;
        }else{
            velocity = 0;
        }
    }

    /**
     * 按下的速度
     * @return 位置变化速度，v=Δx/Δt
     */
    public double PressVelocity(){
        return velocity;
    }

    /**
     * 按下的位置
     * @return 当前位置
     */
    public double PressPosition(){
        return nowPosition;
    }

    /**
     * 刚刚开始使用该按键
     * @return 是否刚开始用
     */
    public boolean justStartUsed(){
        return nowPosition!=0&&lastPosition==0;
    }

    /**
     * 正在使用该按键
     * @return 是否正在使用
     */
    public boolean Using(){return nowPosition!=0;}

    /**
     * 刚刚停止使用该按键
     * @return 是否刚刚停止使用该按键
     */
    public boolean justReleased(){
        return nowPosition==0&&lastPosition!=0;
    }
}
