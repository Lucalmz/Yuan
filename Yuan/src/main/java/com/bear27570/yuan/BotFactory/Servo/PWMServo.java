package com.bear27570.yuan.BotFactory.Servo;

import androidx.annotation.NonNull;

import com.bear27570.yuan.AdvantageCoreLib.Logging.Logger;
import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.bear27570.yuan.BotFactory.Interface.PeriodicRunnable;
import com.bear27570.yuan.BotFactory.Interface.ServoEx;
import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.Model.MotorInformation;
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
public class PWMServo implements RunnableStructUnit, Lockable, ServoEx, PeriodicRunnable {
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
    private final ArrayList<MotorInformation> Config;
    private final HashMap<Action, Double> ServoAction;
    private final HashMap<Action, Double> ServoVelAction;
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
    private boolean isSwitcherAssigned = false;
    public Thread workerThread;
    private final Logger logger;

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
    PWMServo(@NonNull ServoBuilders.PWMServoBuilder Builder) {
        DeviceName = Builder.DeviceName;
        ServoNum = Builder.servoName.size();
        hardwareMap = Builder.hardwareMap;
        this.ServoAction = new HashMap<>(Builder.actionMap);
        this.ServoVelAction = new HashMap<>(Builder.velActionMap);
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
        this.targetVelocityDegPerSec = this.ServoMaxVel;
    }

    private double getCalculatedPosition() {
        return targetPosition;
    }

    public void periodic() {
        logger.logDouble(DeviceName+" "+getConfig(0)+"/commandedPosition", getCalculatedPosition());
        logger.logString(DeviceName+" "+getConfig(0)+"/commandedAction", ServoState.name());
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
            thisActionWaitingSec = TimeServices.GetServoWaitMillSec(TemporaryPosition, this);
            for (int i = 0; i < ServoNum; i++) {
                ControlServo.get(i).setPosition(TemporaryPosition);
                logger.logDouble(DeviceName+" "+getConfig(i)+"/commandedPosition", TemporaryPosition);
                logger.logString(DeviceName+" "+getConfig(i) + "/commandedAction", InTemporary.name());
                logger.logDouble(DeviceName + "/commandedDuration", TimeUnit.MILLISECONDS.toSeconds(thisActionWaitingSec));
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
                SetTemporaryPosition(targetPosition);
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

    /**
     * 设置临时速度
     * @param DegPerSec 目标速度
     */
    public void actWithVel(double DegPerSec) {
        timer.reset();
        setVelocity(DegPerSec);
        logger.logDouble(DeviceName+" "+getConfig(0)+"/commandedVelocity", targetVelocityDegPerSec);
        isVelControlRunning = true;
    }

    public void BlockedActWithVel(double DegPerSec) {
        lock.lock();
        try {
            timer.reset();
            setVelocity(DegPerSec);
            isVelControlRunning = true;
            logger.logDouble(DeviceName+" "+getConfig(0)+"/commandedVelocity", targetVelocityDegPerSec);
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
        logger.logDouble(DeviceName+" "+getConfig(0)+"/commandedVelocity", targetVelocityDegPerSec);
    }

    /**
     * 支持线程安全的动作，使用了ReentrantLock，能够保证动作不被意外打断
     * 所有记录过的舵机动作的入口
     *
     * @param thisAction 当前需要执行的Action类动作名称
     */
    @Override
    public void act(Action thisAction) {
        if (!ServoAction.containsKey(thisAction)&&!ServoVelAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("You used a fucking action that you didn't fucking told me!(｀Д´)");
        }
        lock.lock();
        try {
            if(ServoAction.containsKey(thisAction)) {
                thisActionWaitingSec = TimeServices.GetServoWaitMillSec(thisAction, this);
                for (int i = 0; i < ServoNum; i++) {
                    ControlServo.get(i).setPosition(ServoAction.get(thisAction).doubleValue());
                    logger.logDouble(DeviceName + "/commandedDuration", TimeUnit.MILLISECONDS.toSeconds(thisActionWaitingSec));
                    logger.logDouble(DeviceName + " " + getConfig(i) + "/commandedPosition", ServoAction.get(thisAction).doubleValue());
                }
                ServoState = thisAction;
                currentPosition = ServoAction.get(thisAction).doubleValue();
                return;
            }
            actWithVel(ServoVelAction.get(thisAction).doubleValue());
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
    public double getActionPosition(Action target) {
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
}
