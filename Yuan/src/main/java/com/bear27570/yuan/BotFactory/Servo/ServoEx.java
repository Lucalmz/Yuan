package com.bear27570.yuan.BotFactory.Servo;

import androidx.annotation.NonNull;

import com.bear27570.yuan.AdvantageCoreLib.Logging.Logger;
import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.Model.ConfigDirectionPair;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.Services.ServoVelCalculator;
import com.bear27570.yuan.BotFactory.Services.TimeServices;
import com.bear27570.yuan.BotFactory.Model.SwitcherPair;
import com.bear27570.yuan.BotFactory.ThreadManagement.Task;
import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import static com.bear27570.yuan.BotFactory.Model.Action.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程安全的舵机封装类，使用了ReentrantLock
 *
 * @author LucaLi
 */
@ThreadSafe
public class ServoEx implements RunnableStructUnit, Lockable {
    private final String DeviceName;
    private final ArrayList<Servo> ControlServo = new ArrayList<>();
    private final int ServoNum;
    private final ElapsedTime timer;
    public volatile boolean isVelControlRunning = true;

    private final int updateIntervalMillis = 5;
    // --- 旋转公共API ---
    private volatile double targetVelocityDegPerSec;
    private volatile double targetPosition;
    private volatile double currentPosition;
    private final ArrayList<ConfigDirectionPair> Config;
    private final HashMap<Action, Double> ServoAction;
    private final SwitcherPair switcher;
    protected static HardwareMap hardwareMap;
    private volatile Action ServoState = Init;
    private final Action InitState;
    private final Boolean IsPatienceAvailable;
    private long thisActionWaitingSec;
    private volatile Double ServoMaxVel;
    private final int DegRange;
    private double ServoPosition;
    //并发用
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition movementFinished = lock.newCondition();
    private final PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    private boolean isSwitcherAssigned = false;
    public Thread workerThread;
    private final Logger logger;

    /**
     * 获取等待队列
     */
    public PriorityBlockingQueue<Task> getWaitingQueue() {
        return taskQueue;
    }

    /**
     * 提供公用上锁方法
     */
    public boolean tryLock() {
        return lock.tryLock();
    }

    public void lock() {
        lock.lock();
    }

    /**
     * 提供公用解锁方法
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * 内部构造类
     *
     * @param Builder 实现builder生成器架构
     */
    private ServoEx(@NonNull ServoBuilder Builder) {
        DeviceName = Builder.DeviceName;
        ServoNum = Builder.servoName.size();
        hardwareMap = Builder.hardwareMap;
        this.ServoAction = new HashMap<>(Builder.actionMap);
        Config = new ArrayList<>(Builder.servoName);
        for (int i = 0; i < ServoNum; i++) {
            ControlServo.add(hardwareMap.get(Servo.class, Config.get(i).getConfig()));
            if (Config.get(i).isReverse()) {
                ControlServo.get(i).setDirection(Servo.Direction.REVERSE);
            }
        }
        this.isSwitcherAssigned = Builder.isSwitcherSet;
        this.InitState = Builder.InitState;
        this.switcher = Builder.switcher;
        this.IsPatienceAvailable = Builder.isPatienceAvailable;
        this.ServoMaxVel = Builder.ServoVel;
        this.DegRange = Builder.DegRange;
        this.timer = new ElapsedTime();
        this.workerThread = new Thread(this::velocityControlLoop);
        this.workerThread.setPriority(Thread.MAX_PRIORITY);
        this.workerThread.start();
        this.logger = Logger.getINSTANCE();
    }

    private double getCalculatedPosition() {
        return targetPosition;
    }
    public void Periodic() {

    }

    /**
     * 设置舵机的目标转速。
     *
     * @param degreesPerSecond 目标速度 (度/秒)。正值一个方向，负值反方向。
     */
    public void setVelocity(double degreesPerSecond) {
        this.targetVelocityDegPerSec = degreesPerSecond;

    }

    /**
     * 获取舵机的目标转速。
     */
    public double getVelocity() {
        return this.targetVelocityDegPerSec;
    }

    /**
     * 初始化舵机位置操作
     */
    @Override
    public void Init() {
        for (int i = 0; i < ServoNum; i++) {
            act(InitState);
        }
        ServoState = InitState;
    }

    /**
     * 为视觉这类需要瞄准的提供的方法，能够让舵机在线程安全的情况下到达任意未指定的位置
     *
     * @param TemporaryPosition 舵机需要执行的位置
     */
    public void SetTemporaryPosition(double TemporaryPosition) {
        if (TemporaryPosition < 0 || TemporaryPosition > 1) {
            throw new IllegalArgumentException("Servo position must be between 0.0 and 1.0");
        }
        lock.lock();
        try {
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPosition(TemporaryPosition);
            }
            ServoPosition = TemporaryPosition;
            ServoState = InTemporary;
            currentPosition = TemporaryPosition;
        }finally {
            lock.unlock();
        }
    }

    private void velocityControlLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            if (isVelControlRunning && targetVelocityDegPerSec != 0) {
                targetPosition = ServoVelCalculator.getTargetPosition(timer, targetVelocityDegPerSec, currentPosition, DegRange);
                SetTemporaryPosition(targetPosition);
                if (targetPosition > 1 || targetPosition < 0) {
                    lock.lock();
                    try {
                        isVelControlRunning = false;
                        // signalAll 必须在 try 块中，且必须持有锁
                        movementFinished.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
                currentPosition = targetPosition;
            }
            try {
                Thread.sleep(updateIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void actWithVel(double DegPerSec) {
        timer.reset();
        setVelocity(DegPerSec);
        isVelControlRunning = true;
    }

    public void BlockedActWithVel(double DegPerSec) {
        lock.lock();
        try {
            timer.reset();
            setVelocity(DegPerSec);
            isVelControlRunning = true;
            while (isVelControlRunning) {
                movementFinished.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            lock.unlock();
        }
    }

    public void StopVelTurning() {
        this.isVelControlRunning = false;
        setVelocity(0);
    }

    /**
     * 支持线程安全的动作，使用了ReentrantLock，能够保证动作不被意外打断
     * 所有记录过的舵机动作的入口
     *
     * @param thisAction 当前需要执行的Action类动作名称
     */
    @Override
    public void act(Action thisAction) {
        if (!ServoAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        lock.lock();
        try {
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPosition(ServoAction.get(thisAction));
            }
            thisActionWaitingSec = TimeServices.GetServoWaitMillSec(thisAction, this);
            ServoState = thisAction;
            currentPosition = ServoAction.get(thisAction);
        }finally {
            lock.unlock();
        }
    }

    /**
     * 自带线程阻塞的执行动作
     *
     * @param thisAction 当前目标动作
     * @throws InterruptedException 阻塞可以被打断
     */
    public void PatientAct(Action thisAction) throws InterruptedException {
        lock.lock();
        try {
            if (!IsPatienceAvailable) {
                throw new IllegalArgumentException("You can't use patient act because you haven't registered your servo's velocity");
            }
            act(thisAction);
            TimeUnit.MILLISECONDS.sleep(thisActionWaitingSec);
        }finally {
            lock.unlock();
        }
    }

    /**
     * 获取当前动作需要等待的时间
     */
    public long WaitMillSec() {
        return thisActionWaitingSec;
    }

    /**
     * Switch方法，可以让该舵机在规定的两个状态间切换，若都不在，！会执行到定义的Switch1的状态！
     */
    @Override
    public void Switch() {
        if (isSwitcherAssigned) {
            if (ServoState == switcher.getSwitch1()) {
                act(switcher.getSwitch2());
            } else {
                act(switcher.getSwitch1());
            }
            return;
        }
        throw new IllegalArgumentException("You haven't assigned a switcher for this servo.");
    }

    /**
     * 获取当前舵机动作状态
     *
     * @return Action类型当前动作状态
     */
    @Override
    public Action getState() {
        return ServoState;
    }

    /**
     * 获取第i颗舵机的Config名称，i<n（真的会有人用这个吗？）
     *
     * @param i 第几颗舵机
     * @return 所查询的舵机的Config名称
     */
    @Override
    public String getConfig(int i) {
        if (i >= ServoNum) {
            throw new ArrayIndexOutOfBoundsException("Are you kidding me? I can't tell you a fucking servo name more than" + (ServoNum - 1) + ", but you asked me to tell you the " + i + "one!");
        }
        return Config.get(i).getConfig();
    }

    /**
     * 获取名称和对应数的HashMap
     *
     * @return Hashmap<Action, Double>
     */
    public HashMap<Action, Double> getNameList() {
        return ServoAction;
    }

    /**
     * 获取Action对应的位置
     *
     * @param target 需要获取的Action名称
     * @return 舵机对应的位置
     */
    public Double getActionPosition(Action target) {
        if (target == InTemporary) {
            return ServoPosition;
        }
        return ServoAction.get(target);
    }

    /**
     * 获取舵机角度范围
     *
     * @return 角度范围 Unit:Degree
     */
    public int getDegRange() {
        return DegRange;
    }

    /**
     * 获取舵机最大转速
     *
     * @return 舵机转速（Sec/60°）
     */
    public double getServoMaxVel() {
        return ServoMaxVel;
    }

    /**
     * 获取第i颗舵机是否被设置反向（i<n）
     *
     * @param i 第几颗舵机
     * @return 返回是否被设置反向
     */
    @Override
    public boolean whichIsReversed(int i) {
        if (i >= ServoNum) {
            throw new ArrayIndexOutOfBoundsException("Are you fucking kidding me? I can't tell you a fucking servo whether it is reversed more than" + (ServoNum - 1) + ", but you asked me to tell you the " + i + "one!");
        }
        return Config.get(i).isReverse();
    }

    /**
     * 关闭速度管理线程
     */
    public void shutdownVelThread() {
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt(); // 中断线程
            try {
                workerThread.join(); // 等待线程执行完毕
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 使用了Builder构型，链式调用满足不定项输入需求
     */
    public static class ServoBuilder {
        private String DeviceName;
        private final ArrayList<ConfigDirectionPair> servoName = new ArrayList<>();
        private final Map<Action, Double> actionMap;
        private final HardwareMap hardwareMap;
        private SwitcherPair switcher;
        private final Action InitState;
        private double ServoVel;
        private int DegRange;
        private boolean isPatienceAvailable;
        private boolean isSwitcherSet;

        public ServoBuilder(String ConfigName1, double InitPosition, boolean isReverse, HardwareMap hardwareMap) {
            this.servoName.add(new ConfigDirectionPair(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(Init, InitPosition);
            this.InitState = Init;
            this.hardwareMap = hardwareMap;

        }

        public ServoBuilder(String ConfigName1, Action InitAct, double InitPosition, boolean isReverse, HardwareMap hardwareMap) {
            this.servoName.add(new ConfigDirectionPair(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(InitAct, InitPosition);
            this.InitState = InitAct;
            this.hardwareMap = hardwareMap;
        }
        /**
         * 设置舵机组名称
         */
        public ServoBuilder setDeviceName(String Name){
            DeviceName = Name;
            return this;
        }

        /**
         * 设置该舵机转速以用于自动等待方法
         *
         * @param SecPer60Deg 每60度需要几秒
         * @param DegRange    舵机角度
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder SetServoMaxVelAndRange(double SecPer60Deg, int DegRange) {
            this.ServoVel = SecPer60Deg;
            this.DegRange = DegRange;
            this.isPatienceAvailable = true;
            return this;
        }

        /**
         * 给这个封装添加一个新的同步舵机
         *
         * @param newConfigName 添加舵机的名称
         * @param isReverse     是否反向
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder addServo(String newConfigName, boolean isReverse) {
            servoName.add(new ConfigDirectionPair(newConfigName, isReverse));
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
            if (position < 0.0 || position > 1.0) {
                throw new IllegalArgumentException("Servo position must be between 0.0 and 1.0");
            }
            actionMap.put(actionType, position);
            return this;
        }


        /**
         * 设置便捷转换方式
         *
         * @param switch1 第一个switch需要的动作(任意位置只要调用switch就会回到该位置）
         * @param switch2 第二个switch需要的动作
         * @return 当前Builder实例，实现链式调用
         */
        public ServoBuilder setSwitcher(Action switch1, Action switch2) {
            if (isSwitcherSet) {
                throw new IllegalArgumentException("Switcher should only be assigned for once.");
            }
            switcher = SwitcherPair.GetSwitcherPair(switch1, switch2);
            isSwitcherSet = true;
            return this;
        }

        /**
         * 构建并返回一个 ServoFactory 实例。
         *
         * @return 构建好的 ServoFactory 对象
         */
        public ServoEx build() {
            if (!isSwitcherSet) {
                switcher = SwitcherPair.GetSwitcherPair(null, null);
            }
            return new ServoEx(this);
        }
    }
}
