package com.bear27570.yuan.BotFactory;

import com.qualcomm.robotcore.hardware.PIDFCoefficients;

public class ConfigDirectionPair {
    private final String Config;
    private final PIDFCoefficients PosPIDF;
    private final PIDFCoefficients VelPIDF;
    private final boolean isReverse;
    public ConfigDirectionPair(String Config,boolean isReverse) {
        this.Config = Config;
        this.isReverse = isReverse;
        this.PosPIDF = null;
        this.VelPIDF = null;
    }
    public ConfigDirectionPair(String Config,boolean isReverse,PIDFCoefficients PosPIDF,PIDFCoefficients VelPIDF) {
        this.Config = Config;
        this.isReverse = isReverse;
        this.PosPIDF = PosPIDF;
        this.VelPIDF = VelPIDF;
    }
    public String getConfig() {
        return Config;
    }

    public boolean isReverse() {
        return isReverse;
    }
    public PIDFCoefficients getPosPIDF(){
        return PosPIDF;
    }
    public PIDFCoefficients getVelPIDF(){
        return VelPIDF;
    }
    public boolean isPosPIDFSet(){
        return !(PosPIDF==null);
    }
    public boolean isVelPIDFSet(){
        return !(VelPIDF==null);
    }
}