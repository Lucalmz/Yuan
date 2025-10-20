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
import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.bear27570.yuan.BotFactory.Model.Action.*;

/**
 * A thread-safe wrapper for a standard CRServo that estimates its position based on velocity.
 * This class uses a dedicated background thread for timed motor control, in a style similar to PWMServo.
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
    private final double initPosition;

    // Position and Velocity State
    private volatile double currentPositionDegrees;
    private volatile double targetPositionDegrees;
    private volatile double operatingVelocityDegPerSec = -1.0; // Must be set by user
    private final double maxVelocityDegPerSec;
    private final int degreeRange;
    private long movementDurationMillis = 0;

    // Concurrency and Threading
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition commandReceived = lock.newCondition();
    private final Condition movementFinished = lock.newCondition();
    private final PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    private final Thread workerThread;
    private volatile boolean isMovementRequested = false;

    private final SwitcherPair switcher;
    private final boolean isSwitcherAssigned;
    private final Logger logger;

    public NormalCRServo(@NonNull ServoBuilders.NormalCRServoBuilder builder) {
        this.DeviceName = builder.deviceName;
        hardwareMap = builder.hardwareMap;
        this.positionAction = new HashMap<>(builder.actionMap);
        this.controlServo = hardwareMap.get(CRServo.class, builder.servoName);
        this.isReversed = builder.isReversed;
        if (isReversed) {
            controlServo.setDirection(DcMotorSimple.Direction.REVERSE);
        }

        this.maxVelocityDegPerSec = builder.maxVelocity;
        this.degreeRange = builder.degreeRange;

        this.isSwitcherAssigned = builder.isSwitcherSet;
        this.initState = builder.initState;
        this.initPosition = positionAction.get(initState);

        this.switcher = builder.switcher;
        this.logger = Logger.getINSTANCE();

        // Initialize and start the dedicated worker thread
        this.workerThread = new Thread(this::movementControlLoop);
        this.workerThread.setPriority(Thread.MAX_PRIORITY);
        this.workerThread.start();
    }

    //<editor-fold desc="Locking and Threading Interface">
    @Override
    public PriorityBlockingQueue<Task> getWaitingQueue() {
        return taskQueue;
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

    //<editor-fold desc="Core Control Loop (Worker Thread)">
    private void movementControlLoop() {
        ElapsedTime movementTimer = new ElapsedTime();

        while (!Thread.currentThread().isInterrupted()) {
            double powerToSet = 0;
            long timeToWait = 0;
            double velocityForMove = 0;

            try {
                lock.lock();
                // Wait until a movement is requested
                while (!isMovementRequested) {
                    commandReceived.await();
                }

                // A command was received, prepare for movement
                isMovementRequested = false;
                double distanceToTarget = targetPositionDegrees - currentPositionDegrees;

                if (Math.abs(distanceToTarget) > 0.1) { // Only move if significant
                    powerToSet = Math.signum(distanceToTarget) * (isReversed ? -1 : 1);
                    timeToWait = this.movementDurationMillis;
                    velocityForMove = operatingVelocityDegPerSec;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupted status
                break;
            } finally {
                lock.unlock();
            }

            // Execute the movement outside of the main lock
            if (timeToWait > 0) {
                controlServo.setPower(powerToSet);
                movementTimer.reset();
                try {
                    Thread.sleep(timeToWait);
                    // Movement completed successfully
                    lock.lock();
                    try {
                        currentPositionDegrees = targetPositionDegrees;
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    // Movement was interrupted by StopVelTurning() or a new command
                    lock.lock();
                    try {
                        // Update position to where it actually got
                        double timeElapsedSec = movementTimer.seconds();
                        double distanceMoved = timeElapsedSec * velocityForMove * Math.signum(powerToSet) * (isReversed ? -1 : 1);
                        currentPositionDegrees += distanceMoved;
                    } finally {
                        lock.unlock();
                    }
                    Thread.currentThread().interrupt(); // Preserve interrupted status
                } finally {
                    controlServo.setPower(0);
                    lock.lock();
                    try {
                        // Signal any blocking threads that this movement (or its interruption) is complete
                        movementFinished.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            } else {
                // If no movement was needed, immediately signal completion
                lock.lock();
                try {
                    movementFinished.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Public Control Methods">
    @Override
    public void Init() {
        lock.lock();
        try {
            StopVelTurning();
            this.currentPositionDegrees = this.initPosition;
            this.targetPositionDegrees = this.initPosition;
            this.servoState = initState;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void periodic() {
        // This method is called from an external loop, used here for logging.
        // The core logic is in the worker thread.
        logger.logDouble(DeviceName + "/estimatedPositionDeg", this.currentPositionDegrees);
        logger.logDouble(DeviceName + "/powerApplied", controlServo.getPower());
        logger.logString(DeviceName + "/currentState", servoState.name());
    }

    @Override
    public void act(Action thisAction) {
        if (!positionAction.containsKey(thisAction)) {
            throw new IllegalArgumentException("Action " + thisAction.name() + " is not defined for " + DeviceName);
        }
        double targetPos = positionAction.get(thisAction);
        SetTemporaryPosition(targetPos);
        servoState = thisAction;
    }

    @Override
    public void SetTemporaryPosition(double temporaryPosition) {
        if (operatingVelocityDegPerSec <= 0) {
            throw new IllegalStateException("Operating velocity must be set via setVelocity() before moving.");
        }
        lock.lock();
        try {
            StopVelTurning(); // Interrupts any ongoing movement
            this.targetPositionDegrees = temporaryPosition;
            this.movementDurationMillis = WaitMillSec();

            logger.logDouble(DeviceName + "/commandedTargetPosition", targetPositionDegrees);
            logger.logDouble(DeviceName + "/commandedVelocity", operatingVelocityDegPerSec);
            logger.logDouble(DeviceName + "/calculatedDurationMs", movementDurationMillis);

            isMovementRequested = true;
            servoState = InTemporary;
            commandReceived.signal(); // Wake up the worker thread
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void actWithVel(double DegPerSec) {
        setVelocity(DegPerSec);
        // This method starts a movement towards the last set target position with a new velocity.
        SetTemporaryPosition(this.targetPositionDegrees);
    }

    @Override
    public void BlockedActWithVel(double DegPerSec) {
        lock.lock();
        try {
            setVelocity(DegPerSec);
            SetTemporaryPosition(this.targetPositionDegrees);
            // Wait until the worker thread signals that movement is finished
            movementFinished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void PatientAct(Action thisAction) throws InterruptedException {
        long waitTime;
        lock.lock();
        try {
            act(thisAction);
            waitTime = this.movementDurationMillis;
        } finally {
            lock.unlock();
        }
        // Sleep on the calling thread, separate from the worker
        if (waitTime > 0) {
            Thread.sleep(waitTime);
        }
    }

    @Override
    public void StopVelTurning() {
        lock.lock();
        try {
            controlServo.setPower(0);
            isMovementRequested = false;
            workerThread.interrupt(); // This will break the Thread.sleep() in the worker
        } finally {
            lock.unlock();
        }
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
    //</editor-fold>

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
    public long WaitMillSec() {
        if (operatingVelocityDegPerSec <= 0) return 0;
        // This calculation is now internal and must be done under lock
        lock.lock();
        try {
            return (long) ((Math.abs(targetPositionDegrees - currentPositionDegrees) / operatingVelocityDegPerSec) * 1000.0);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getActionPosition(Action target) {
        return positionAction.getOrDefault(target, 0.0);
    }

    @Override
    public double getServoMaxVel() {
        return this.maxVelocityDegPerSec;
    }

    @Override
    public int getDegRange() {
        return this.degreeRange;
    }

    @Override
    public double getVelocity() {
        return this.operatingVelocityDegPerSec;
    }

    @Override
    public void setVelocity(double degreesPerSecond) {
        if (degreesPerSecond <= 0) {
            this.operatingVelocityDegPerSec = -1; // Invalidate velocity
            StopVelTurning();
        } else if (degreesPerSecond > maxVelocityDegPerSec) {
            this.operatingVelocityDegPerSec = maxVelocityDegPerSec;
        } else {
            this.operatingVelocityDegPerSec = degreesPerSecond;
        }
    }

    @Override
    public HashMap<Action, Double> getNameList() {
        return this.positionAction;
    }

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