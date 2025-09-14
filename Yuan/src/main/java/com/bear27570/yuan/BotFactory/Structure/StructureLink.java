package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Interface.LockableGroup;
import com.bear27570.yuan.BotFactory.Model.RSUActPair;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.Model.StructureActionPair;
import com.bear27570.yuan.BotFactory.Model.SwitcherPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
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
public class StructureLink implements LockableGroup {
    private final ArrayList<RunnableStructUnit> StructureRSU;
    private final ArrayList<Action> ActionList;
    private final HashMap<Action, StructureActionPair> SafetyCheckStructureList;
    private final HashMap<Action, RSUActPair> SafetyCheckUnitList;
    private final int SafetyCheckUnitNum;
    private final int StructureRSUNum;
    private final int SafetyCheckStructureNum;
    private final SwitcherPair Switcher;
    public ReentrantLock lock = new ReentrantLock();
    private volatile Action State;

    /**
     * 内部构造类
     * @param Builder 实现builder生成器架构
     */
    private StructureLink(@NonNull StructureBuilder Builder) {
        this.StructureRSU=Builder.RSU;
        this.ActionList = Builder.ActList;
        this.SafetyCheckStructureList = Builder.SafetyCheckStructure;
        this.SafetyCheckStructureNum = SafetyCheckStructureList.size();
        this.SafetyCheckUnitList = Builder.SafetyCheckUnitList;
        this.SafetyCheckUnitNum = SafetyCheckUnitList.size();
        this.StructureRSUNum=StructureRSU.size();
        this.Switcher = Builder.Switcher;
        this.State = Builder.InitState;
    }

    /**
     * 将直系子系统全部上锁
     */
    @Override
    public void lockAllSubsystems(){
        for(int i = 0; i < StructureRSUNum;i++){
            Objects.requireNonNull(StructureRSU.get(i)).lock();
        }
    }

    /**
     * 解锁全部直系子系统
     */
    @Override
    public void unLockAllSubsystems(){
        for(int i = 0; i < StructureRSUNum;i++){
            Objects.requireNonNull(StructureRSU.get(i)).unlock();
        }
    }
    /**
     * 将全部子系统全部上锁
     * @param currentAct 当前的执行动作，用于确认SafetyCheck所需的结构，避免等待不需要的结构
     */
    public void lockAllWithSafetyCheckSubsystems(Action currentAct){
        for (int i = 0; i < SafetyCheckStructureNum; i++) {
            Objects.requireNonNull(SafetyCheckStructureList.get(currentAct)).getStructure().lock.lock();
            Objects.requireNonNull(SafetyCheckStructureList.get(currentAct)).getStructure().lockAllWithSafetyCheckSubsystems(currentAct);
        }
        for (int i = 0; i < SafetyCheckUnitNum;i++){
            Objects.requireNonNull(SafetyCheckUnitList.get(currentAct)).getUnit().lock();
        }
        lockAllSubsystems();
    }
    /**
     * 释放所有子系统的锁
     * @param currentAct 当前的执行动作，用于确认SafetyCheck所需的结构，避免等待不需要的结构
     */
    public void unLockAllWithSafetyCheckSubsystems(Action currentAct){
        for (int i = 0; i < SafetyCheckStructureNum; i++) {
            Objects.requireNonNull(SafetyCheckStructureList.get(currentAct)).getStructure().lock.unlock();
            Objects.requireNonNull(SafetyCheckStructureList.get(currentAct)).getStructure().unLockAllWithSafetyCheckSubsystems(currentAct);
        }
        for (int i = 0; i < SafetyCheckUnitNum;i++){
            Objects.requireNonNull(SafetyCheckUnitList.get(currentAct)).getUnit().unlock();
        }
        unLockAllSubsystems();
    }
    /**
     * 主动作函数，使整个结构所有部件都到达对应的动作映射
     * @param currentAct 需要执行的动作名称
     */
    public void act(Action currentAct) {
        lock.lock();
        lockAllWithSafetyCheckSubsystems(currentAct);
        try {
            for (int i = 0; i < SafetyCheckStructureNum; i++) {
                SafetyCheckStructureList.get(currentAct).run();
            }
            for (int i = 0; i < SafetyCheckUnitNum; i++){
                SafetyCheckUnitList.get(currentAct).run();
            }
            for (int i = 0; i < StructureRSUNum; i++){
                StructureRSU.get(i).act(currentAct);
            }
            State = currentAct;
        }finally {
            lock.unlock();
            unLockAllWithSafetyCheckSubsystems(currentAct);
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
        for (int i = 0; i < StructureRSUNum; i++) {
            for (int m = 0; i < ActionList.size(); i++) {
                if (!StructureRSU.get(i).getNameList().containsKey(ActionList.get(m))) {
                    throw new IllegalArgumentException("Your structure has a unit called" + StructureRSU.get(i).getConfig(0) + " that didn't have a public action name registered on its own list.");
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
        lockAllWithSafetyCheckSubsystems(Aim);
        try {
            if (requestState != State) {
                return;
            }
            act(Aim);
        }finally {
            lock.unlock();
            unLockAllWithSafetyCheckSubsystems(Aim);
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
        lockAllWithSafetyCheckSubsystems(Aim);
        try {
            if (requestState != State) {
                return;
            }
            act(Aim);
        }finally {
            lock.unlock();
            unLockAllWithSafetyCheckSubsystems(Aim);
        }
    }

    public static class StructureBuilder {
        private ArrayList<RunnableStructUnit>RSU;
        private ArrayList<Action> ActList;
        private HashMap<Action, StructureActionPair> SafetyCheckStructure;
        private HashMap<Action, RSUActPair> SafetyCheckUnitList;
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
         * 添加一个结构单元
         *
         * @param newRSU 这个结构中的一个结构单元
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(RunnableStructUnit newRSU) {
            this.RSU.add(newRSU);
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
         * @param AttachUnit 需要被安全检查的结构单元
         * @param SafeAct     该舵机需要的状态
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder addStructActSafetyCheck(Action StructAct, RunnableStructUnit AttachUnit, Action SafeAct) {
            SafetyCheckUnitList.put(StructAct, new RSUActPair(AttachUnit, SafeAct));
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
         * 构建并返回一个 StructureLink 实例。
         *
         * @return 构建好的 StructureLink 对象
         */
        public StructureLink build() {
            return new StructureLink(this);
        }
    }
}