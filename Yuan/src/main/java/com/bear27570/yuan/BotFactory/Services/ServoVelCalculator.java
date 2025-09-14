package com.bear27570.yuan.BotFactory.Services;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
//针对舵机速度的计算位置功能
public class ServoVelCalculator {
    public static double getTargetPosition(ElapsedTime timer,double targetVelocity, double currentPosition,double DegRange) {
        double elapsedSeconds = timer.seconds();
        timer.reset();
        double targetPosition;

        // 如果速度为0或时间间隔太长，则不进行更新
        if (targetVelocity == 0 || elapsedSeconds > 0.2) {
            return currentPosition;
        }

        // 计算这段时间内应该移动的角度
        double deltaDegrees = targetVelocity * elapsedSeconds;

        // 将当前位置(0.0-1.0)转换为角度
        double currentDegrees = currentPosition * DegRange;

        // 计算新的目标角度
        double newTargetDegrees = currentDegrees + deltaDegrees;

        // 限制新角度在舵机的物理范围内 (0 到 servoRangeDegrees)
        newTargetDegrees = Math.max(0.0, Math.min(newTargetDegrees, DegRange));

        // 将计算出的新角度转换回舵机的 (0.0-1.0) 位置值
        targetPosition = newTargetDegrees / DegRange;

        // 6. 向舵机发送最终的位置指令
        return targetPosition;
    }
    //工具类不应实例化
    private ServoVelCalculator(){}
}
