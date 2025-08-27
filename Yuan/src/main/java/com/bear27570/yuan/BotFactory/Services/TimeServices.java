package com.bear27570.yuan.BotFactory.Services;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Servo.CRServoEx;
import com.bear27570.yuan.BotFactory.Servo.ServoEx;

/**
 * 提供计算时间相关的功能函数
 */
public class TimeServices {
    /**
     * 获取舵机需要运行的时间
     * @param TargetState 当前需要到达的目标状态
     * @param MoveServo 执行的舵机
     * @return 需要运行的时间 Unit:MillSecond
     */
    public static long GetServoWaitMillSec(Action TargetState, ServoEx MoveServo){
        double MovePosition = MoveServo.getActionPosition(MoveServo.getState())-MoveServo.getActionPosition(TargetState);
        return (long) (Math.abs(MovePosition)*(MoveServo.getServoVel()/(MoveServo.getDegRange()/60.0))*1000);
    }
    public static long GetServoWaitMillSec(Action TargetState, CRServoEx MoveServo){
        double MovePosition = MoveServo.getActionPosition(MoveServo.getState())-MoveServo.getActionPosition(TargetState);
        return (long) (Math.abs(MovePosition)*(MoveServo.getServoVel()/(MoveServo.getDegRange()/60.0))*1000);
    }
    /**
     * 私有构造函数，防止实例化
     */
    private TimeServices(){}
}
