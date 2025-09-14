package com.bear27570.yuan.BotFactory.Model;

import com.qualcomm.robotcore.hardware.PIDFCoefficients;
/**
 * 记录Config和是否反向的共同封装类，用于设置反向
 * 添加记录pidf的信息
 */
public class ConfigDirectionPair {
    private final String Config;
    private final PIDFCoefficients PosPIDF;
    private final PIDFCoefficients VelPIDF;
    private final boolean isReverse;
    //简单创建法
    public ConfigDirectionPair(String Config,boolean isReverse) {
        this.Config = Config;
        this.isReverse = isReverse;
        this.PosPIDF = null;
        this.VelPIDF = null;
    }
    //带pid的创建
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
    //以下是对于两个PIDF的信息获取
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