package com.bear27570.yuan.BotFactory.Motor;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.ConfigDirectionPair;
import com.bear27570.yuan.BotFactory.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.SwitcherPair;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.BiMap;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import static com.bear27570.yuan.BotFactory.Action.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
/**
 * 线程安全的DC电机封装类，使用了ReentrantLock
 * @author LucaLi
 */
public class  MotorFactory implements RunnableStructUnit {
    private final ArrayList<DcMotorEx> ControlMotor= new ArrayList<>();
    private final int MotorNum;
    private final ArrayList<ConfigDirectionPair> Config;
    private final HashMap<Action, Integer> MotorAction;
    protected static HardwareMap hardwareMap;
    private volatile Action MotorState = Init;
    private final Action InitState;
    private final SwitcherPair switcher;
    public ReentrantLock lock = new ReentrantLock();
    private final boolean isSwitcherAssigned;
    /**
     * 内部构造类
     * @param Builder 实现builder生成器架构
     */
    public MotorFactory(@NonNull MotorBuilder Builder){
        MotorNum=Builder.MotorName.size();
        hardwareMap = Builder.hardwareMap;
        this.MotorAction = new HashMap<>(Builder.actionMap);
        Config = new ArrayList<>(Builder.MotorName);
        for(int i = 0;i < MotorNum;i++){
            ControlMotor.add(hardwareMap.get(DcMotorEx.class,Config.get(i).getConfig()));
            if(Config.get(i).isReverse()){
                ControlMotor.get(i).setDirection(DcMotorSimple.Direction.REVERSE);
            }
        }
        isSwitcherAssigned = Builder.isSwitcherSet;
        InitState = Builder.InitState;
        switcher = Builder.switcher;
    }

    /**
     * 初始化电机操作
     * ZeroPowerBehavior Brake
     */
    @Override
    public void Init(){
        for(int i = 0;i < MotorNum; i++){
            ControlMotor.get(i).setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            ControlMotor.get(i).setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            if(Config.get(i).isPosPIDFSet()){
                ControlMotor.get(i).setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION,Config.get(i).getPosPIDF());
            }
            if(Config.get(i).isVelPIDFSet()){
                ControlMotor.get(i).setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,Config.get(i).getVelPIDF());
            }
        }
        MotorState = InitState;
    }
    /**
     *支持线程安全的动作，使用了ReentrantLock，能够保证动作不被意外打断
     * power默认为1的电机动作的入口
     * @param thisAction 当前需要执行的Action类动作名称
     */
    @Override
    public void act(Action thisAction){
       actWithPowerLimit(thisAction,1);
    }

    /**
     * 支持功率限制的动作
     * 支持线程安全的动作，使用了ReentrantLock，能够保证动作不被意外打断
     *  所有记录过的机动作的入口
     * @param thisAction 当前需要执行的Action类动作名称
     * @param powerLimit 功率限制
     */
    public void actWithPowerLimit(Action thisAction,double powerLimit){
        Action requestState = MotorState;
        lock.lock();
        try {
            if(requestState!=MotorState){
                return;
            }
            if (!MotorAction.containsKey(thisAction)) {
                throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
            }
            for (int i = 0; i < MotorNum; i++) {
                ControlMotor.get(i).setTargetPosition(MotorAction.get(thisAction));
                ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
                ControlMotor.get(i).setPower(powerLimit);
            }
            MotorState = thisAction;
        }finally {
            lock.unlock();
        }
    }
    /**
     * 直接设置功率
     * @param Power 功率
     */
    public void setPower(double Power){
        lock.lock();
        try {
            for (int i = 0; i < MotorNum; i++) {
                ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                ControlMotor.get(i).setPower(Power);
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * 获取转速
     * @return 所有电机平均转速
     */
    public double getVelocity(){
        double averageVelocity = 0;
        for (int i = 0; i < MotorNum; i++){
            averageVelocity += ControlMotor.get(i).getVelocity();
        }
        averageVelocity/= MotorNum;
        return averageVelocity;
    }
    /**
     * 为视觉这类需要瞄准的提供的方法，能够让电机在线程安全的情况下到达任意未指定的位置
     * @param TemporaryPosition 电机需要执行的位置
     */
    public void setTemporaryPosition(int TemporaryPosition){
        lock.lock();
        try {
            for (int i = 0; i < MotorNum; i++) {
                ControlMotor.get(i).setTargetPosition(TemporaryPosition);
                ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
            }
            MotorState = InTemporary;
        }finally {
            lock.unlock();
        }
    }
    /**
     * Switch方法，可以让该电机在规定的两个状态间切换，若都不在，！会执行到定义的Switch1的状态！
     */
    @Override
    public void Switch(){
        if(isSwitcherAssigned) {
            if (MotorState == switcher.getSwitch1()) {
                act(switcher.getSwitch2());
                return;
            }
            act(switcher.getSwitch1());
            return;
        }
        throw new IllegalArgumentException("You haven't assigned a switcher for those motors.");
    }
    /**
     * 获取当前电机动作状态
     * @return Action类型当前动作状态
     */
    @Override
    public Action getState(){
        return MotorState;
    }
    /**
     * 获取名称和对应数的HashMap（自己去查啊！）
     * @return Hashmap<Action,Double>
     */
    public HashMap<Action,Integer> getNameList(){
        return MotorAction;
    }
    /**
     * 获取第i颗电机的Config名称，i<n（真的会有人用这个吗？）
     * @param i 第几颗电机
     * @return 所查询的电机的Config名称
     */
    @Override
    public String getConfig(int i){
        if(i>=MotorNum){
            throw new ArrayIndexOutOfBoundsException("Are you kidding me? I can't tell you a fucking servo name more than"+(MotorNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).getConfig();
    }
    /**
     * 获取第i颗电机是否被设置反向（i<n）
     * @param i 第几颗电机
     * @return 返回是否被设置反向
     */
    @Override
    public boolean whichIsReversed(int i){
        if(i>=MotorNum){
            throw new ArrayIndexOutOfBoundsException("Are you fucking kidding me? I can't tell you a fucking servo whether it is reversed more than"+(MotorNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).isReverse();
    }
    /**
     * 使用了Builder构型，链式调用满足不定项输入需求
     */
    public static class MotorBuilder {
        private ArrayList<ConfigDirectionPair> MotorName = new ArrayList<>();
        private Map<Action, Integer> actionMap;
        private final HardwareMap hardwareMap;
        private SwitcherPair switcher;
        private final Action InitState;
        private boolean isSwitcherSet,isSinglePIDFSet;
        public MotorBuilder(String ConfigName1,int InitPosition,boolean isReverse,HardwareMap hardwareMap) {
            this.MotorName.add(new ConfigDirectionPair(ConfigName1,isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(Init,InitPosition);
            this.InitState = Init;
            this.hardwareMap = hardwareMap;
        }
        public MotorBuilder(String ConfigName1,Action InitAct,int InitPosition,boolean isReverse,HardwareMap hardwareMap) {
            this.MotorName.add(new ConfigDirectionPair(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(InitAct, InitPosition);
            this.InitState = InitAct;
            this.hardwareMap = hardwareMap;
        }
        /**
         *给这个封装添加一个新的同步电机
         *
         * @param newConfigName 添加电机的名称
         * @param isReverse 是否反向
         * @return 当前Builder实例，实现链式调用
         */
        public MotorBuilder addMotor(String newConfigName,boolean isReverse){
            MotorName.add(new ConfigDirectionPair(newConfigName,isReverse));
            return this;
        }

        /**
         * 添加并设置电机RUN-To-Position模式的PIDF
         * @param newConfigName 新电机的名字
         * @param isReverse 是否反向
         * @param PosPIDF PIDF参数
         * @return 返回当前实例，实现链式调用
         */
        public MotorBuilder addMotorWithPosPIDF(String newConfigName,boolean isReverse,PIDFCoefficients PosPIDF){
            MotorName.add(new ConfigDirectionPair(newConfigName,isReverse,PosPIDF,null));
            return this;
        }
        /**
         * 添加并设置电机RUN-With-Encoder模式的PIDF
         * @param newConfigName 新电机的名字
         * @param isReverse 是否反向
         * @param VelPIDF PIDF参数
         * @return 返回当前实例，实现链式调用
         */
        public MotorBuilder addMotorWithVelPIDF(String newConfigName,boolean isReverse,PIDFCoefficients VelPIDF){
            MotorName.add(new ConfigDirectionPair(newConfigName,isReverse,null,VelPIDF));
            return this;
        }
        /**
         * 添加并设置电机两个模式的PIDF
         * @param newConfigName 新电机的名字
         * @param isReverse 是否反向
         * @param PosPIDF RUN-To-Position的PIDF参数
         * @param VelPIDF RUN-With-Encoder的PIDF参数
         * @return 返回当前实例，实现链式调用
         */
        public MotorBuilder addMotorWithPIDF(String newConfigName,boolean isReverse,PIDFCoefficients PosPIDF,PIDFCoefficients VelPIDF){
            MotorName.add(new ConfigDirectionPair(newConfigName,isReverse,PosPIDF,VelPIDF));
            return this;
        }
        /**
         * 添加一个动作及其对应的Motor位置。
         *
         * @param actionType 动作的枚举类型
         * @param position   Servo的目标位置 (通常0.0到1.0之间)
         * @return 当前Builder实例，实现链式调用
         */
        public MotorBuilder addAction(Action actionType, int position) {
            actionMap.put(actionType, position);
            return this;
        }
        /**
         * 设置电机switch方法（只能调用一遍）
         * @param switch1 设置第一个switch动作（所有其他位置时调用switch都会切到该位置）
         * @param switch2 设置第二个switch动作
         * @return 当前Builder实例，实现链式调用
         */
        public MotorBuilder setSwitcher(Action switch1,Action switch2){
            if(isSwitcherSet){
                throw new IllegalArgumentException("Switcher should only be assigned for once.");
            }
            switcher = SwitcherPair.GetSwitcherPair(switch1,switch2);
            isSwitcherSet = true;
            return this;
        }

        /**
         * 构建并返回一个 ServoFactory 实例。
         *
         * @return 构建好的 ServoFactory 对象
         */
        public MotorFactory build() {
            if(!isSwitcherSet){
                switcher = SwitcherPair.GetSwitcherPair(null,null);
            }
            return new MotorFactory(this);
        }
    }
}