package com.bear27570.yuan.BotFactory.Motor;

import com.bear27570.yuan.BotFactory.Interface.DcMotorIO;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

public class RealDcMotor implements DcMotorIO {
    private final DcMotorEx motor;
    public RealDcMotor(String motorName, HardwareMap hardwareMap) {
        this.motor = hardwareMap.get(DcMotorEx.class, motorName);
    }

    @Override
    public void setPower(double power) {
        motor.setPower(power);
    }

    @Override
    public void setVelocity(double velocity) {
        motor.setVelocity(velocity);
    }

    @Override
    public void setTargetPosition(int position) {
        motor.setTargetPosition(position);
    }

    @Override
    public void setMode(DcMotor.RunMode mode) {
        motor.setMode(mode);
    }

    @Override
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        motor.setZeroPowerBehavior(behavior);
    }

    @Override
    public void setPIDFCoefficients(DcMotor.RunMode mode, PIDFCoefficients coefficients) {
        motor.setPIDFCoefficients(mode, coefficients);
    }

    @Override
    public void setDirection(DcMotor.Direction direction) {
        motor.setDirection(direction);
    }

    @Override
    public DcMotor.RunMode getMode() {
        return motor.getMode();
    }

    @Override
    public boolean isBusy() {
        return motor.isBusy();
    }

    @Override
    public double getVelocity() {
        return motor.getVelocity();
    }

    @Override
    public int getCurrentPosition() {
        return motor.getCurrentPosition();
    }

    @Override
    public double getPower() {
        return motor.getPower();
    }

    @Override
    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior() {
        return motor.getZeroPowerBehavior();
    }
}
