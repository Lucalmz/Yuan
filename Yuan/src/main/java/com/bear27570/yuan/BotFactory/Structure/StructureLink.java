package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.CRServoActPair;
import com.bear27570.yuan.BotFactory.LockableUnit;
import com.bear27570.yuan.BotFactory.Motor.MotorEx;
import com.bear27570.yuan.BotFactory.MotorActPair;
import com.bear27570.yuan.BotFactory.Servo.CRServoFactory;
import com.bear27570.yuan.BotFactory.Servo.ServoEx;
import com.bear27570.yuan.BotFactory.ServoActPair;
import com.bear27570.yuan.BotFactory.StructureActionPair;
import com.bear27570.yuan.BotFactory.SwitcherPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
/*
 *                        _oo0oo_
 *                       o8888888o
 *                       88" . "88
 *                       (| -_- |)
 *                       0\  =  /0
 *                     ___/`---'\___
 *                   .' \\|     |// '.
 *                  / \\|||  :  |||// \
 *                 / _||||| -:- |||||- \
 *                |   | \\\  - /// |   |
 *                | \_|  ''\---/''  |_/ |
 *                \  .-\__  '-'  ___/-. /
 *              ___'. .'  /--.--\  `. .'___
 *           ."" '<  `.___\_<|>_/___.' >' "".
 *          | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *          \  \ `_.   \_ __\ /__ _/   .-` /  /
 *      =====`-.____`.___ \_____/___.-`___.-'=====
 *                        `=---='
 *
 *
 *      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *            佛祖保佑       永不宕机     永无BUG
 */
public class StructureLink implements LockableUnit {
    private final ArrayList<ServoEx> StructureServo;
    private final ArrayList<MotorEx> StructureMotor;
    private final ArrayList<CRServoFactory> StructureCRServo;
    private final ArrayList<Action> ActionList;
    private final HashMap<Action, ServoActPair> SafetyCheckServoList;
    private final HashMap<Action, MotorActPair> SafetyCheckMotorList;
    private final HashMap<Action, StructureActionPair> SafetyCheckStructureList;
    private final HashMap<Action, CRServoActPair> SafetyCheckCRServoList;
    private final int ServoNum;
    private final int MotorNum;
    private final int CRServoNum;
    private final int SafetyCheckServoNum;
    private final int SafetyCheckMotorNum;
    private final int SafetyCheckStructureNum;
    private final int SafetyCheckCRServoNum;
    private final SwitcherPair Switcher;
    public ReentrantLock lock = new ReentrantLock();
    private volatile Action State;

    /**
     * 内部构造类
     * @param Builder 实现builder生成器架构
     */
    private StructureLink(@NonNull StructureBuilder Builder) {
        this.StructureServo = Builder.Servo;
        this.StructureMotor = Builder.Motor;
        this.StructureCRServo = Builder.CRServo;
        this.ActionList = Builder.ActList;
        this.SafetyCheckServoList = Builder.SafetyCheckServo;
        this.SafetyCheckMotorList = Builder.SafetyCheckMotor;
        this.SafetyCheckStructureList = Builder.SafetyCheckStructure;
        this.SafetyCheckCRServoList = Builder.SafetyCheckCRServo;
        this.ServoNum = StructureServo.size();
        this.MotorNum = StructureMotor.size();
        this.CRServoNum = StructureCRServo.size();
        this.SafetyCheckServoNum = SafetyCheckServoList.size();
        this.SafetyCheckMotorNum = SafetyCheckMotorList.size();
        this.SafetyCheckStructureNum = SafetyCheckStructureList.size();
        this.SafetyCheckCRServoNum = SafetyCheckCRServoList.size();
        this.Switcher = Builder.Switcher;
        this.State = Builder.InitState;
    }

    /**
     * 将子系统全部上锁
     * @param currentAct 当前的执行动作，用于确认SafetyCheck所需的结构，避免等待不需要的结构
     */
    public void lockAllSubsystem(Action currentAct){
        for (int i = 0; i < SafetyCheckStructureNum; i++) {
            SafetyCheckStructureList.get(currentAct).getStructure().lock.lock();
        }
        for (int i = 0; i < SafetyCheckServoNum; i++) {
            SafetyCheckServoList.get(currentAct).getServo().lock.lock();
        }
        for (int i = 0; i < SafetyCheckMotorNum; i++) {
            SafetyCheckMotorList.get(currentAct).getMotor().lock.lock();
        }
        for (int i = 0; i < SafetyCheckCRServoNum; i++) {
            SafetyCheckCRServoList.get(currentAct).getServo().lock.lock();
        }
        lockAllSubsystems();
    }
    /**
     * 将所有直系子系统上锁
     **/
    public void lockAllSubsystems(){
        for (int i = 0; i < ServoNum; i++) {
            StructureServo.get(i).lock.lock();
        }
        for (int i = 0; i < MotorNum; i++) {
            StructureMotor.get(i).lock.lock();
        }
        for (int i = 0; i < CRServoNum; i++) {
            StructureCRServo.get(i).lock.lock();
        }
    }
    /**
     * 释放所有子系统的锁
     * @param currentAct 当前的执行动作，用于确认SafetyCheck所需的结构，避免等待不需要的结构
     */
    public void unLockAllSubsystem(Action currentAct){
        for (int i = 0; i < SafetyCheckStructureNum; i++) {
            SafetyCheckStructureList.get(currentAct).getStructure().lock.unlock();
        }
        for (int i = 0; i < SafetyCheckServoNum; i++) {
            SafetyCheckServoList.get(currentAct).getServo().lock.unlock();
        }
        for (int i = 0; i < SafetyCheckMotorNum; i++) {
            SafetyCheckMotorList.get(currentAct).getMotor().lock.unlock();
        }
        for (int i = 0; i < SafetyCheckCRServoNum; i++) {
            SafetyCheckCRServoList.get(currentAct).getServo().lock.unlock();
        }
        unLockAllSubsystems();
    }

    /**
     * 释放所有直系子系统的锁
     */
    public void unLockAllSubsystems(){
        for (int i = 0; i < ServoNum; i++) {
            StructureServo.get(i).lock.unlock();
        }
        for (int i = 0; i < MotorNum; i++) {
            StructureMotor.get(i).lock.unlock();
        }
        for (int i = 0; i < CRServoNum; i++) {
            StructureCRServo.get(i).lock.unlock();
        }
    }
    /**
     * 主动作函数，使整个结构所有部件都到达对应的动作映射
     * @param currentAct 需要执行的动作名称
     */
    public void act(Action currentAct) {
        lock.lock();
        lockAllSubsystem(currentAct);
        try {
            for (int i = 0; i < SafetyCheckStructureNum; i++) {
                SafetyCheckStructureList.get(currentAct).run();
            }
            for (int i = 0; i < SafetyCheckServoNum; i++) {
                SafetyCheckServoList.get(currentAct).run();
            }
            for (int i = 0; i < SafetyCheckMotorNum; i++) {
                SafetyCheckMotorList.get(currentAct).run();
            }
            for (int i = 0; i < SafetyCheckCRServoNum; i++) {
                SafetyCheckCRServoList.get(currentAct).run();
            }
            for (int i = 0; i < ServoNum; i++) {
                StructureServo.get(i).act(currentAct);
            }
            for (int i = 0; i < MotorNum; i++) {
                StructureMotor.get(i).act(currentAct);
            }
            for (int i = 0; i < CRServoNum; i++) {
                StructureCRServo.get(i).act(currentAct);
            }
            State = currentAct;
        }finally {
            lock.unlock();
            unLockAllSubsystem(currentAct);
        }
    }

    /**
     * 提供结构统一初始化方法，可以减少初始化代码负担
     */
    public void Init(){
        Check();
        act(State);
    }

    /**
     * 检查该结构是否都有对应的动作名称映射
     */
    public void Check() {
        for (int i = 0; i < ServoNum; i++) {
            for (int m = 0; i < ActionList.size(); i++) {
                if (!StructureServo.get(i).getNameList().containsKey(ActionList.get(m))) {
                    throw new IllegalArgumentException("Your structure has a servo called" + StructureServo.get(i).getConfig(0) + " that didn't have a public action name registered on its own list.");
                }
            }
        }
        for (int i = 0; i < MotorNum; i++) {
            for (int m = 0; i < ActionList.size(); i++) {
                if (!StructureMotor.get(m).getNameList().containsKey(ActionList.get(i))) {
                    throw new IllegalArgumentException("Your structure has a motor called" + StructureMotor.get(i).getConfig(0) + " that didn't have a public action name registered on its own list.");
                }
            }
        }
        for (int i = 0; i < CRServoNum; i++) {
            for (int m = 0; i < ActionList.size(); i++) {
                if (!StructureCRServo.get(m).getNameList().containsKey(ActionList.get(i))) {
                    throw new IllegalArgumentException("Your structure has a CR servo called" + StructureCRServo.get(i).getConfig(0) + " that didn't have a public action name registered on its own list.");
                }
            }
        }
    }

    /**
     * 强大的交换,若不在switch的第二状态则一律执行到第一状态
     */
    public void StrongSwitch() {
        Action requestState = State;
        Action Aim = Switcher.getSwitch1();
        if(State == Switcher.getSwitch1()){
            Aim = Switcher.getSwitch2();
        }
        //双重锁，保证子结构绝对受控
        lock.lock();
        lockAllSubsystem(Aim);
        try {
            if (requestState != State) {
                return;
            }
            act(Aim);
        }finally {
            lock.unlock();
            unLockAllSubsystem(Aim);
        }
    }

    /**
     * 严苛的交换，如果该structure不在标记的应在的状态时报错
     */
    public void StrictSwitch() {
        Action requestState = State;
        Action Aim = null;
        if (State == Switcher.getSwitch1()) {
            Aim=Switcher.getSwitch2();
        }
        if (State == Switcher.getSwitch2()) {
            Aim=Switcher.getSwitch1();
        }
        if(Aim == null){
            throw new IllegalStateException("This Structure isn't in any state of those two registered state!");
        }
        lock.lock();
        lockAllSubsystem(Aim);
        try {
            if (requestState != State) {
                return;
            }
            act(Aim);
        }finally {
            lock.unlock();
            unLockAllSubsystem(Aim);
        }
    }

    public static class StructureBuilder {
        private ArrayList<ServoEx> Servo;
        private ArrayList<MotorEx> Motor;
        private ArrayList<CRServoFactory> CRServo;
        private ArrayList<Action> ActList;
        private HashMap<Action, ServoActPair> SafetyCheckServo;
        private HashMap<Action, MotorActPair> SafetyCheckMotor;
        private HashMap<Action, CRServoActPair> SafetyCheckCRServo;
        private HashMap<Action, StructureActionPair> SafetyCheckStructure;
        private SwitcherPair Switcher;
        private Action InitState = Action.Init;
        private boolean switcherIsSet;

        /**
         * 空参构造，无特殊必须的参数
         */
        public StructureBuilder() {
        }

        /**
         * 设置初始的Action类型状态名称
         * @param initState 初始的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder setInitialState(Action initState) {
            InitState = initState;
            return this;
        }

        /**
         * 添加一个舵机
         *
         * @param newServo 这个结构中的一个舵机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(ServoEx newServo) {
            this.Servo.add(newServo);
            return this;
        }

        /**
         * 添加一个电机
         *
         * @param newMotor 这个结构中的一个电机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(MotorEx newMotor) {
            this.Motor.add(newMotor);
            return this;
        }

        /**
         * 添加一个CR舵机
         *
         * @param newCRServo 这个结构中的一个CR舵机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(CRServoFactory newCRServo) {
            this.CRServo.add(newCRServo);
            return this;
        }

        /**
         * 为当前结构添加动作
         *
         * @param act 需要添加的动作（所有结构中的部件都要设定过这个动作！）
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructureAction(Action act) {
            this.ActList.add(act);
            return this;
        }

        /**
         * 设置便捷转换方式
         *
         * @param a1 第一个switch需要的动作(任意位置只要调用StrongSwitch就会回到该位置）
         * @param a2 第二个switch需要的动作
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder setSwitcher(Action a1, Action a2) {
            if (switcherIsSet) {
                throw new IllegalArgumentException("Switcher should only be assigned for once.");
            }
            switcherIsSet = true;
            Switcher = SwitcherPair.GetSwitcherPair(a1, a2);
            return this;
        }

        /**
         * 为当前结构添加安全性检查，添加执行动作时结构外的可能对该结构动作产生影响的结构所应处于的位置
         *
         * @param StructAct   当执行该动作时需要安全检查
         * @param AttachServo 需要被安全检查的舵机
         * @param SafeAct     该舵机需要的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructActSafetyCheck(Action StructAct, ServoEx AttachServo, Action SafeAct) {
            SafetyCheckServo.put(StructAct, new ServoActPair(AttachServo, SafeAct));
            return this;
        }

        /**
         * 为当前结构添加安全性检查，添加执行动作时结构外的可能对该结构动作产生影响的结构所应处于的位置
         *
         * @param StructAct   当执行该动作时需要安全检查
         * @param AttachMotor 需要被安全检查的舵机
         * @param SafeAct     该舵机需要的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructActSafetyCheck(Action StructAct, MotorEx AttachMotor, Action SafeAct) {
            SafetyCheckMotor.put(StructAct, new MotorActPair(AttachMotor, SafeAct));
            return this;
        }

        /**
         * 为当前结构添加安全性检查，添加执行动作时结构外的可能对该结构动作产生影响的结构所应处于的位置
         *
         * @param StructAct    当执行该动作时需要安全检查
         * @param AttachStruct 需要被安全检查的结构组
         * @param SafeAct      该结构组需要的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructActSafetyCheck(Action StructAct, StructureLink AttachStruct, Action SafeAct) {
            SafetyCheckStructure.put(StructAct, new StructureActionPair(AttachStruct, SafeAct));
            return this;
        }

        /**
         * 为当前结构添加安全性检查，添加执行动作时结构外的可能对该结构动作产生影响的结构所应处于的位置
         *
         * @param StructAct   当执行该动作时需要安全检查
         * @param AttachServo 需要被安全检查的CR舵机
         * @param SafeAct     该CR舵机需要的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructActSafetyCheck(Action StructAct, CRServoFactory AttachServo, Action SafeAct) {
            SafetyCheckCRServo.put(StructAct, new CRServoActPair(AttachServo, SafeAct));
            return this;
        }

        /**
         * 构建并返回一个 StructureLink 实例。
         *
         * @return 构建好的 StructureLink 对象
         */
        public StructureLink build() {
            return new StructureLink(this);
        }
    }
}