package com.bear27570.yuan.BotFactory.Services;

public class MotorVelocityCalculator {
    public static double OutputVelocityToRoundPerSec(double outputVelocity,double wheelDiameter){
        return outputVelocity*Math.PI*wheelDiameter;
    }
    private MotorVelocityCalculator(){}
}
