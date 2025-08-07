package com.bear27570.yuan.BotFactory.Motor;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.ConfigDirectionPair;
import com.bear27570.yuan.BotFactory.SwitcherPair;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import static com.bear27570.yuan.BotFactory.Action.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class  MotorFactory {
    private final ArrayList<DcMotorEx> ControlMotor= new ArrayList<>();
    private final int MotorNum;
    private final ArrayList<ConfigDirectionPair> Config;
    private final Map<Action, Integer> MotorAction;
    protected static HardwareMap hardwareMap;
    private final Map<Action,Action> SwitcherLink;
    private Action MotorState = Init;
    private final SwitcherPair switcher;
    private final boolean isSwitcherAssigned;
    public MotorFactory(@NonNull MotorBuilder Builder){
        MotorNum=Builder.MotorName.size();
        hardwareMap = Builder.hardwareMap;
        this.MotorAction = Collections.unmodifiableMap(new HashMap<>(Builder.actionMap));
        Config = new ArrayList<>(Builder.MotorName);
        for(int i = 0;i < MotorNum;i++){
            ControlMotor.add(hardwareMap.get(DcMotorEx.class,Config.get(i).getConfig()));
            if(Config.get(i).isReverse()){
                ControlMotor.get(i).setDirection(DcMotorSimple.Direction.REVERSE);
            }
        }
        SwitcherLink = Builder.switcherLinkArrayList;
        isSwitcherAssigned = Builder.isSwitcherSet;
        switcher = Builder.switcher;
    }
    public void Init(){
        for(int i = 0;i < MotorNum; i++){
            ControlMotor.get(i).setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            ControlMotor.get(i).setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        MotorState = Init;
    }
    public void act(Action thisAction){
        if(!MotorAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        for (int i = 0; i < MotorNum; i++) {
            ControlMotor.get(i).setTargetPosition(MotorAction.get(thisAction));
            ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
            ControlMotor.get(i).setPower(1);
        }
        MotorState =thisAction;
    }
    public void setPower(double Power){
        for (int i = 0; i < MotorNum; i++) {
            ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            ControlMotor.get(i).setPower(Power);
        }
    }
    public double getVelocity(){
        double averageVelocity = 0;
        for (int i = 0; i < MotorNum; i++){
            averageVelocity += ControlMotor.get(i).getVelocity();
        }
        averageVelocity/= MotorNum;
        return averageVelocity;
    }
    public void setTemporaryPosition(int TemporaryPosition){
        for (int i = 0; i < MotorNum; i++) {
            ControlMotor.get(i).setTargetPosition(TemporaryPosition);
            ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        MotorState = InTemporary;
    }
    public void Switch(){
        if(SwitcherLink.containsKey(MotorState)){
            for (int i = 0; i < MotorNum; i++) {
                ControlMotor.get(i).setTargetPosition(MotorAction.get(SwitcherLink.get(MotorState)));
                ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
            }
            MotorState = SwitcherLink.get(MotorState);
            return;
        }
        if(isSwitcherAssigned){
            if(MotorState==switcher.getSwitch1()){
                for (int i = 0; i < MotorNum; i++) {
                    ControlMotor.get(i).setTargetPosition(MotorAction.get(switcher.getSwitch2()));
                    ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
                }
                MotorState = switcher.getSwitch2();
            }else {
                for (int i = 0; i < MotorNum; i++) {
                    ControlMotor.get(i).setTargetPosition(MotorAction.get(switcher.getSwitch1()));
                    ControlMotor.get(i).setMode(DcMotor.RunMode.RUN_TO_POSITION);
                }
                MotorState = switcher.getSwitch1();
            }
        }else {
            throw new IllegalArgumentException("You haven't assigned a switcher for those motors.");
        }
    }
    public Action getState(){
        return MotorState;
    }
    public String getConfig(int i){
        if(i>=MotorNum){
            throw new ArrayIndexOutOfBoundsException("Are you kidding me? I can't tell you a fucking servo name more than"+(MotorNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).getConfig();
    }
    public boolean whichIsReversed(int i){
        if(i>=MotorNum){
            throw new ArrayIndexOutOfBoundsException("Are you fucking kidding me? I can't tell you a fucking servo whether it is reversed more than"+(MotorNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).isReverse();
    }
    public static class MotorBuilder {
        private final ArrayList<ConfigDirectionPair> MotorName = new ArrayList<>();
        private final Map<Action, Integer> actionMap;
        private final HardwareMap hardwareMap;
        private SwitcherPair switcher;
        private Map<Action,Action> switcherLinkArrayList;
        private boolean isSwitcherSet;
        public MotorBuilder(String ConfigName1,int InitPosition,boolean isReverse,HardwareMap hardwareMap) {
            this.MotorName.add(new ConfigDirectionPair(ConfigName1,isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(Init,InitPosition);
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
         *设置优先连接的switch方法
         * @param LinkAction1 第一个互联的动作
         * @param LinkAction2 第二个互联的动作
         * @return 当前Builder实例，实现链式调用
         */
        public MotorBuilder setSwitcherLink(Action LinkAction1,Action LinkAction2){
            switcherLinkArrayList.put(LinkAction1,LinkAction2);
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