package com.bear27570.yuan.BotFactory.Interface;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

public interface DcMotorIO {
    //----output----
    void setPower(double power);
    void setVelocity(double velocity);
    void setTargetPosition(int position);
    void setMode(DcMotor.RunMode mode);
    void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior);
    void setPIDFCoefficients(DcMotor.RunMode mode, PIDFCoefficients coefficients);
    void setDirection(DcMotor.Direction direction);

    //----input----
    DcMotor.RunMode getMode();
    boolean isBusy();
    double getVelocity();
    int getCurrentPosition();
    double getPower();
    DcMotor.ZeroPowerBehavior getZeroPowerBehavior();
}
