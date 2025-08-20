package com.bear27570.yuan.BotFactory.Servo;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.ConfigDirectionPair;
import com.bear27570.yuan.BotFactory.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.SwitcherPair;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import static com.bear27570.yuan.BotFactory.Action.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class  CRServoFactory implements RunnableStructUnit {
    private final ArrayList<CRServo> ControlServo= new ArrayList<>();
    private final int ServoNum;
    private final ArrayList<ConfigDirectionPair> Config;
    private final HashMap<Action, Double> ServoAction;
    private final SwitcherPair switcher;
    protected static HardwareMap hardwareMap;
    private Action ServoState = Init;
    private final Action InitState;
    private boolean isSwitcherAssigned = false;
    private CRServoFactory(@NonNull ServoBuilder Builder){
        ServoNum=Builder.servoName.size();
        hardwareMap = Builder.hardwareMap;
        this.ServoAction = new HashMap<>(Builder.actionMap);
        Config = new ArrayList<>(Builder.servoName);
        for(int i = 0;i < ServoNum;i++){
            ControlServo.add(hardwareMap.get(CRServo.class,Config.get(i).getConfig()));
            if(Config.get(i).isReverse()){
                ControlServo.get(i).setDirection(CRServo.Direction.REVERSE);
            }
        }
        isSwitcherAssigned = Builder.isSwitcherSet;
        InitState = Builder.InitState;
        switcher = Builder.switcher;
    }
    public void Init(){
        for(int i = 0;i < ServoNum; i++){
            act(InitState);
        }
        ServoState = InitState;
    }
    public void SetTemporaryPosition(double TemporaryPosition){
        for(int i = 0;i < ServoNum; i++){
            ControlServo.get(i).setPower(TemporaryPosition);
        }
        ServoState = InTemporary;
    }
    public void act(Action thisAction){
        if(!ServoAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        for (int i = 0; i < ServoNum; i++) {
            ControlServo.get(i).setPower(ServoAction.get(thisAction));
        }
        ServoState=thisAction;
    }
    public void Switch(){
        if(isSwitcherAssigned){
            if(ServoState==switcher.getSwitch1()){
                for (int i = 0; i < ServoNum; i++) {
                    ControlServo.get(i).setPower(ServoAction.get(switcher.getSwitch2()));
                }
                ServoState = switcher.getSwitch2();
            }else {
                for (int i = 0; i < ServoNum; i++) {
                    ControlServo.get(i).setPower(ServoAction.get(switcher.getSwitch1()));
                }
                ServoState = switcher.getSwitch1();
            }
        }else {
            throw new IllegalArgumentException("You haven't assigned a switcher for this servo.");
        }
    }
    public Action getState(){
        return ServoState;
    }
    public HashMap<Action,Double> getNameList(){
        return ServoAction;
    }
    public String getConfig(int i){
        if(i>=ServoNum){
            throw new ArrayIndexOutOfBoundsException("Are you kidding me? I can't tell you a fucking servo name more than"+(ServoNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).getConfig();
    }
    public boolean whichIsReversed(int i){
        if(i>=ServoNum){
            throw new ArrayIndexOutOfBoundsException("Are you fucking kidding me? I can't tell you a fucking servo whether it is reversed more than"+(ServoNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).isReverse();
    }
    public static class ServoBuilder {
        private final ArrayList<ConfigDirectionPair> servoName = new ArrayList<>();
        private final Map<Action, Double> actionMap;
        private final HardwareMap hardwareMap;
        private SwitcherPair switcher;
        private final Action InitState;
        private boolean isSwitcherSet;
        public ServoBuilder(String ConfigName1,double InitPosition,boolean isReverse,HardwareMap hardwareMap) {
            this.servoName.add(new ConfigDirectionPair(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(Init, InitPosition);
            this.InitState = Init;
            this.hardwareMap = hardwareMap;
        }
        public ServoBuilder(String ConfigName1,Action InitAct,double InitPosition,boolean isReverse,HardwareMap hardwareMap) {
            this.servoName.add(new ConfigDirectionPair(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(InitAct, InitPosition);
            this.InitState = InitAct;
            this.hardwareMap = hardwareMap;
        }
        /**
         *给这个封装添加一个新的同步舵机
         *
         * @param newConfigName 添加舵机的名称
         * @param isReverse 是否反向
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder addServo(String newConfigName,boolean isReverse){
            servoName.add(new ConfigDirectionPair(newConfigName,isReverse));
            return this;
        }

        /**
         * 添加一个动作及其对应的Servo位置。
         *
         * @param actionType 动作的枚举类型
         * @param position   Servo的目标位置 (通常0.0到1.0之间)
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder addAction(Action actionType, double position) {
            if (Math.abs(position)>1) {
                throw new IllegalArgumentException("Servo position must be between -1.0 and 1.0");
            }
            actionMap.put(actionType, position);
            return this;
        }

        /**
         *设置便捷转换方式
         * @param switch1 第一个switch需要的动作(任意位置只要调用switch就会回到该位置）
         * @param switch2 第二个switch需要的动作
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder setSwitcher(Action switch1,Action switch2){
            if(isSwitcherSet){
                throw new IllegalArgumentException("Switcher should only be assigned for once.");
            }
            switcher = SwitcherPair.GetSwitcherPair(switch1,switch2);
            isSwitcherSet = true;
            return this;
        }
        /**
         * 构建并返回一个 ServoFactory 实例。
         * @return 构建好的 ServoFactory 对象
         */
        public CRServoFactory build() {
            if(!isSwitcherSet){
                switcher = SwitcherPair.GetSwitcherPair(null,null);
            }
            return new CRServoFactory(this);
        }
    }
}
