package com.bear27570.yuan.BotFactory.Servo;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Model.ConfigDirectionPair;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.Model.SwitcherPair;
import com.bear27570.yuan.BotFactory.Services.TimeServices;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;

import static com.bear27570.yuan.BotFactory.Action.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
/**
 * 线程安全的CR舵机封装类，使用了ReentrantLock
 * @author LucaLi
 */
public class CRServoEx implements RunnableStructUnit {
    private final ArrayList<CRServo> ControlServo= new ArrayList<>();
    private final int ServoNum;
    private final ArrayList<ConfigDirectionPair> Config;
    private final HashMap<Action, Double> ServoAction;
    private final SwitcherPair switcher;
    protected static HardwareMap hardwareMap;
    private volatile Action ServoState = Init;
    private final Action InitState;
    private final boolean IsPatienceAvailable;
    private long thisActionWaitingSec;
    private final double ServoVel;
    private final int DegRange;
    private double ServoPosition;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean isSwitcherAssigned = false;

    public void lock(){
        lock.lock();
    }
    public void unlock(){
        lock.unlock();
    }
    private CRServoEx(@NonNull ServoBuilder Builder){
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
        ServoVel = Builder.ServoVel;
        IsPatienceAvailable = Builder.isPatienceAvailable;
        DegRange=Builder.DegRange;
    }
    /**
     * 初始化舵机位置操作
     */
    @Override
    public void Init(){
        for(int i = 0;i < ServoNum; i++){
            act(InitState);
        }
        ServoState = InitState;
    }
    /**
     * 为视觉这类需要瞄准的提供的方法，能够让舵机在线程安全的情况下到达任意未指定的位置
     * @param TemporaryPosition 舵机需要执行的位置
     */
    public void SetTemporaryPosition(double TemporaryPosition){
        lock.lock();
        try {
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPower(TemporaryPosition);
            }
            ServoPosition=TemporaryPosition;
            ServoState = InTemporary;
        }finally {
            lock.unlock();
        }
    }
    /**
     *支持线程安全的动作，使用了ReentrantLock，能够保证动作不被意外打断
     * 所有记录过的舵机动作的入口
     * @param thisAction 当前需要执行的Action类动作名称
     */
    @Override
    public void act(Action thisAction){
        if(!ServoAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        lock.lock();
        try {
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPower(ServoAction.get(thisAction));
            }
            thisActionWaitingSec = TimeServices.GetServoWaitMillSec(thisAction,this);
            ServoState = thisAction;
        }finally {
            lock.unlock();
        }
    }
    /**
     * 自带线程阻塞的执行动作
     * @param thisAction 当前目标动作
     * @throws InterruptedException 阻塞可以被打断
     */
    public void PatientAct(Action thisAction) throws InterruptedException {
        if(!IsPatienceAvailable){
            throw new IllegalArgumentException("You can't use patient act because you haven't registered your servo's velocity");
        }
        if(!ServoAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        //给动作上锁，以免导致线程抢舵机
        lock.lock();
        try {
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPower(ServoAction.get(thisAction));
            }
            thisActionWaitingSec = TimeServices.GetServoWaitMillSec(thisAction,this);
            TimeUnit.MILLISECONDS.sleep(thisActionWaitingSec);
            ServoState = thisAction;
        }finally {
            lock.unlock();
        }
    }
    /**
     * 获取当前动作需要等待的时间
     */
    public long WaitMillSec(){
        return thisActionWaitingSec;
    }
    /**
     * Switch方法，可以让该舵机在规定的两个状态间切换，若都不在，！会执行到定义的Switch1的状态！
     */
    @Override
    public void Switch(){
        if(isSwitcherAssigned){
            Action requestState = ServoState;
            lock.lock();
            try {
                if(requestState!=ServoState){
                    return;
                }
                if (ServoState == switcher.getSwitch1()) {
                    act(switcher.getSwitch2());
                } else {
                    act(switcher.getSwitch1());
                }
            }finally {
                lock.unlock();
            }
        }else {
            throw new IllegalArgumentException("You haven't assigned a switcher for this servo.");
        }
    }

    /**
     * 获取舵机角度范围
     * @return 角度范围 Unit:Degree
     */
    public int getDegRange(){
        return DegRange;
    }
    /**
     * 获取舵机转速
     * @return 舵机转速（Sec/60°）
     */
    public double getServoVel(){
        return ServoVel;
    }
    /**
     * 获取Action对应的位置
     * @param target 需要获取的Action名称
     * @return 舵机对应的位置
     */
    public Double getActionPosition(Action target){
        if(target==InTemporary){
            return ServoPosition;
        }
        return ServoAction.get(target);
    }
    /**
     * 获取当前舵机动作状态
     * @return Action类型当前动作状态
     */
    @Override
    public Action getState(){
        return ServoState;
    }
    /**
     * 获取名称和对应数的HashMap（自己去查啊！）
     * @return Hashmap<Action,Double>
     */
    public HashMap<Action,Double> getNameList(){
        return ServoAction;
    }
    /**
     * 获取第i颗舵机的Config名称，i<n（真的会有人用这个吗？）
     * @param i 第几颗舵机
     * @return 所查询的舵机的Config名称
     */
    @Override
    public String getConfig(int i){
        if(i>=ServoNum){
            throw new ArrayIndexOutOfBoundsException("Are you kidding me? I can't tell you a fucking servo name more than"+(ServoNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).getConfig();
    }
    /**
     * 获取第i颗舵机是否被设置反向（i<n）
     * @param i 第几颗舵机
     * @return 返回是否被设置反向
     */
    public boolean whichIsReversed(int i){
        if(i>=ServoNum){
            throw new ArrayIndexOutOfBoundsException("Are you fucking kidding me? I can't tell you a fucking servo whether it is reversed more than"+(ServoNum-1)+", but you asked me to tell you the "+i+"one!");
        }
        return Config.get(i).isReverse();
    }
    /**
     * 使用了Builder构型，链式调用满足不定项输入需求
     */
    public static class ServoBuilder {
        private final ArrayList<ConfigDirectionPair> servoName = new ArrayList<>();
        private final Map<Action, Double> actionMap;
        private final HardwareMap hardwareMap;
        private SwitcherPair switcher;
        private double ServoVel;
        private int DegRange;
        private boolean isPatienceAvailable;
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
         * 设置该舵机转速以用于自动等待方法
         * @param SecPer60Deg 每60度需要几秒
         * @param DegRange 舵机角度
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder SetServoVelAndRange(double SecPer60Deg, int DegRange){
            this.ServoVel = SecPer60Deg;
            this.DegRange = DegRange;
            this.isPatienceAvailable=true;
            return this;
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
        public CRServoEx build() {
            if(!isSwitcherSet){
                switcher = SwitcherPair.GetSwitcherPair(null,null);
            }
            return new CRServoEx(this);
        }
    }
}
