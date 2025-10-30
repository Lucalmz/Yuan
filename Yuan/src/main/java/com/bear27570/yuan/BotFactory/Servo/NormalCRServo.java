package com.bear27570.yuan.BotFactory.Servo;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.bear27570.yuan.BotFactory.Interface.PeriodicRunnable;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;
import com.bear27570.yuan.BotFactory.Interface.ServoEx;
import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.Model.SwitcherPair;
import com.bear27570.yuan.BotFactory.ThreadManagement.Task;
import com.bear27570.yuan.AdvantageCoreLib.Logging.Logger;
import com.bear27570.yuan.BotFactory.Services.TimeServices;
import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.bear27570.yuan.BotFactory.Model.Action.*;

/**
 * A thread-safe wrapper for a CRServo that estimates position and mimics the control logic of PWMServo.
 * It uses a dedicated background thread for continuous velocity and position management.
 *
 * @author LucaLi
 */
@ThreadSafe
public class NormalCRServo implements ServoEx, PeriodicRunnable, RunnableStructUnit, Lockable {
    private final String DeviceName;
    private final CRServo controlServo;
    private final boolean isReversed;
    private final HashMap<Action, Double> positionAction;
    protected static HardwareMap hardwareMap;

    private volatile Action servoState = Init;
    private final Action initState;

    // --- Control State ---
    private volatile double currentPositionDegrees;
    private volatile double targetPositionDegrees;
    private volatile double targetVelocityDegPerSec;
    private final double ServoMaxVel;
    private final int DegRange = Integer.MAX_VALUE; // CR Servos have a virtually infinite range
    private long thisActionWaitingSec = 0;
    public volatile boolean isVelControlRunning = false;
    private volatile boolean isPositionMovementActive = false;
    private final int updateIntervalMillis = 10; // Update rate for the control loop

    // --- Concurrency & Threading ---
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition movementFinished = lock.newCondition();
    public Thread workerThread;

    private final SwitcherPair switcher;
    private final boolean isSwitcherAssigned;
    private final Logger logger;
    private final ElapsedTime timer;

    public NormalCRServo(@NonNull ServoBuilders.NormalCRServoBuilder builder) {
        this.DeviceName = builder.deviceName;
        hardwareMap = builder.hardwareMap;
        this.positionAction = new HashMap<>(builder.actionMap);
        this.controlServo = hardwareMap.get(CRServo.class, builder.servoName);
        this.isReversed = builder.isReversed;
        if (isReversed) {
            controlServo.setDirection(DcMotorSimple.Direction.REVERSE);
        }

        this.ServoMaxVel = builder.maxVelocity;
        this.targetVelocityDegPerSec = this.ServoMaxVel; // Default to max velocity

        this.isSwitcherAssigned = builder.isSwitcherSet;
        this.initState = builder.initState;
        // Set initial position based on initState
        this.currentPositionDegrees = positionAction.getOrDefault(initState, 0.0);
        this.targetPositionDegrees = this.currentPositionDegrees;

        this.switcher = builder.switcher;
        this.logger = Logger.getINSTANCE();
        this.timer = new ElapsedTime();

        // Initialize and start the dedicated worker thread
        this.workerThread = new Thread(this::velocityControlLoop);
        this.workerThread.setPriority(Thread.MAX_PRIORITY);
        this.workerThread.start();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }
    //</editor-fold>

    private void velocityControlLoop() {
        timer.reset();
        while (!Thread.currentThread().isInterrupted()) {
            double powerToSet = 0.0;
            double timeElapsedSec = timer.seconds();
            timer.reset();

            lock.lock();
            try {
                // Update estimated position based on last cycle's power
                double lastPower = controlServo.getPower();
                if (Math.abs(lastPower) > 0.01) {
                    double direction = Math.signum(lastPower) * (isReversed ? -1 : 1);
                    currentPositionDegrees += timeElapsedSec * targetVelocityDegPerSec * direction;
                }

                // --- Determine motor power for this cycle ---
                if (isVelControlRunning) {
                    // Mode 1: Velocity Control
                    powerToSet = Math.signum(targetVelocityDegPerSec);
                } else if (isPositionMovementActive) {
                    // Mode 2: Position Control
                    double positionError = targetPositionDegrees - currentPositionDegrees;
                    if (Math.abs(positionError) > 1.0) { // Tolerance of 1.0 degree
                        powerToSet = Math.signum(positionError);
                    } else {
                        // Target reached
                        isPositionMovementActive = false;
                        movementFinished.signalAll(); // Signal completion
                    }
                }

                if (isReversed) {
                    powerToSet *= -1;
                }
                controlServo.setPower(powerToSet);

            } finally {
                lock.unlock();
            }

            try {
                Thread.sleep(updateIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Ensure motor is off on exit
        controlServo.setPower(0);
    }

    @Override
    public void periodic() {
        logger.logDouble(DeviceName + "/estimatedPositionDeg", this.currentPositionDegrees);
        logger.logString(DeviceName + "/commandedAction", servoState.name());
        logger.logDouble(DeviceName + "/powerApplied", controlServo.getPower());
    }

    @Override
    public void Init() {
        act(initState);
        servoState = initState;
    }

    @Override
    public void act(Action thisAction) {
        if (!positionAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("Action " + thisAction.name() + " is not defined for " + DeviceName);
        }
        lock.lock();
        try {
            // Stop any velocity-based movement
            this.isVelControlRunning = false;
            double targetPos = positionAction.get(thisAction);
            this.targetPositionDegrees = targetPos;
            this.thisActionWaitingSec = (long) ((Math.abs(targetPositionDegrees - currentPositionDegrees) / ServoMaxVel) * 1000.0);
            this.isPositionMovementActive = true;
            this.servoState = thisAction;

            logger.logDouble(DeviceName + "/commandedDuration", TimeUnit.MILLISECONDS.toSeconds(thisActionWaitingSec));
            logger.logDouble(DeviceName + "/commandedPosition", targetPositionDegrees);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void SetTemporaryPosition(double temporaryPosition) {
        lock.lock();
        try {
            this.isVelControlRunning = false;
            this.targetPositionDegrees = temporaryPosition;
            this.thisActionWaitingSec = (long) ((Math.abs(targetPositionDegrees - currentPositionDegrees) / ServoMaxVel) * 1000.0);
            this.isPositionMovementActive = true;
            this.servoState = InTemporary;

            logger.logDouble(DeviceName + "/commandedDuration", TimeUnit.MILLISECONDS.toSeconds(thisActionWaitingSec));
            logger.logDouble(DeviceName + "/commandedPosition", temporaryPosition);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void actWithVel(double DegPerSec) {
        lock.lock();
        try {
            isPositionMovementActive = false;
            setVelocity(DegPerSec);
            logger.logDouble(DeviceName + " " + getConfig(0) + "/commandedVelocity", targetVelocityDegPerSec);
            isVelControlRunning = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void BlockedActWithVel(double DegPerSec) {
        lock.lock();
        try {
            actWithVel(DegPerSec);
            // Block until isVelControlRunning is set to false by StopVelTurning()
            while (isVelControlRunning) {
                movementFinished.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void StopVelTurning() {
        lock.lock();
        try {
            if (isVelControlRunning) {
                this.isVelControlRunning = false;
                movementFinished.signalAll(); // Signal blocked calls to wake up
            }
            isPositionMovementActive = false;
            controlServo.setPower(0);
            logger.logDouble(DeviceName + " " + getConfig(0) + "/commandedVelocity", 0);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void PatientAct(Action thisAction) throws InterruptedException {
        act(thisAction);
        // Sleep on the calling thread for the calculated duration
        // The actual movement is handled by the background thread
        TimeUnit.MILLISECONDS.sleep(WaitMillSec());
    }

    @Override
    public void Switch() {
        if (!isSwitcherAssigned) {
            throw new IllegalArgumentException("You haven't assigned a switcher for this servo.");
        }
        if (servoState == switcher.getSwitch1()) {
            act(switcher.getSwitch2());
        } else {
            act(switcher.getSwitch1());
        }
    }

    @Override
    public long WaitMillSec() {
        // Return the duration calculated when the last action was set
        return thisActionWaitingSec;
    }

    //<editor-fold desc="State and Information Getters">
    @Override
    public Action getState() {
        return servoState;
    }

    @Override
    public String getConfig(int i) {
        if (i > 0) throw new ArrayIndexOutOfBoundsException("This class only supports one servo.");
        return controlServo.getDeviceName();
    }

    @Override
    public boolean whichIsReversed(int i) {
        if (i > 0) throw new ArrayIndexOutOfBoundsException("This class only supports one servo.");
        return isReversed;
    }

    @Override
    public double getActionPosition(Action target) {
        if (target == InTemporary) {
            return targetPositionDegrees;
        }
        return positionAction.getOrDefault(target, 0.0);
    }

    @Override
    public double getServoMaxVel() {
        return this.ServoMaxVel;
    }

    @Override
    public int getDegRange() {
        return this.DegRange;
    }

    @Override
    public double getVelocity() {
        return this.targetVelocityDegPerSec;
    }

    @Override
    public void setVelocity(double degreesPerSecond) {
        this.targetVelocityDegPerSec = Math.min(Math.abs(degreesPerSecond), ServoMaxVel);
    }

    @Override
    public HashMap<Action, Double> getNameList() {
        return this.positionAction;
    }
    //</editor-fold>

    @Override
    public void shutdownVelThread() {
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100); // Wait briefly for the thread to die
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}