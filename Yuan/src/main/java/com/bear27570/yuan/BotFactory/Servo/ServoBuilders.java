package com.bear27570.yuan.BotFactory.Servo;

import static com.bear27570.yuan.BotFactory.Model.Action.Init;

import com.bear27570.yuan.BotFactory.Model.Action;
import com.bear27570.yuan.BotFactory.Model.MotorInformation;
import com.bear27570.yuan.BotFactory.Model.SwitcherPair;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServoBuilders {
    public static class PWMServoBuilder {
        protected String DeviceName;
        protected final ArrayList<MotorInformation> servoName = new ArrayList<>();
        protected final Map<Action, Double> actionMap;
        protected final Map<Action, Double> velActionMap = new HashMap<>();
        protected final HardwareMap hardwareMap;
        protected SwitcherPair switcher;
        protected final Action InitState;
        protected double ServoVel;
        protected int DegRange;
        protected boolean isPatienceAvailable;
        protected boolean isSwitcherSet;

        public PWMServoBuilder(String ConfigName1, double InitPosition, boolean isReverse, HardwareMap hardwareMap) {
            this.servoName.add(new MotorInformation(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(Init, InitPosition);
            this.InitState = Init;
            this.hardwareMap = hardwareMap;
        }

        public PWMServoBuilder(String ConfigName1, Action InitAct, double InitPosition, boolean isReverse, HardwareMap hardwareMap) {
            this.servoName.add(new MotorInformation(ConfigName1, isReverse));
            this.actionMap = new HashMap<>();
            this.actionMap.put(InitAct, InitPosition);
            this.InitState = InitAct;
            this.hardwareMap = hardwareMap;
        }
        /**
         * 设置舵机组名称
         */
        public PWMServoBuilder setDeviceName(String Name){
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
        public PWMServoBuilder SetServoMaxVelAndRange(double SecPer60Deg, int DegRange) {
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
        public PWMServoBuilder addServo(String newConfigName, boolean isReverse) {
            servoName.add(new MotorInformation(newConfigName, isReverse));
            return this;
        }

        /**
         * 添加一个动作及其对应的Servo位置。
         *
         * @param actionType 动作的枚举类型
         * @param position   Servo的目标位置 (通常0.0到1.0之间)
         * @return 当前Builder实例，实现链式调用
         */
        public PWMServoBuilder addAction(Action actionType, double position) {
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
        public PWMServoBuilder setSwitcher(Action switch1, Action switch2) {
            if (isSwitcherSet) {
                throw new IllegalArgumentException("Switcher should only be assigned for once.");
            }
            switcher = SwitcherPair.GetSwitcherPair(switch1, switch2);
            isSwitcherSet = true;
            return this;
        }

        /**
         * 添加对应值为速度的动作
         * @param action 对应的动作名称
         * @param velocity 速度值(deg/sec)
         * @return 当前Builder实例，实现链式调用
         */
        public PWMServoBuilder addVelAction(Action action, double velocity){
            velActionMap.put(action, velocity);
            return this;
        }

        /**
         * 构建并返回一个 ServoFactory 实例。
         *
         * @return 构建好的 ServoFactory 对象
         */
        public PWMServo build() {
            if (!isSwitcherSet) {
                switcher = SwitcherPair.GetSwitcherPair(null, null);
            }
            return new PWMServo(this);
        }
    }
    public static class CRServoBuilder {

    }
    public static class NormalCRServoBuilder {
        protected String deviceName;
        protected final String servoName;
        protected boolean isReversed = false;
        protected final Map<Action, Double> actionMap;
        protected final HardwareMap hardwareMap;
        protected SwitcherPair switcher;
        protected final Action initState;
        protected boolean isSwitcherSet = false;

        protected final double maxVelocity; // in degrees per second

        public NormalCRServoBuilder(String servoName, Action initAct, double initPosition, boolean isReversed, double maxVelocity, HardwareMap hardwareMap) {
            if (maxVelocity <= 0) throw new IllegalArgumentException("Max velocity must be a positive value.");
            this.servoName = servoName;
            this.isReversed = isReversed;
            this.maxVelocity = maxVelocity;
            this.hardwareMap = hardwareMap;
            this.actionMap = new HashMap<>();
            this.actionMap.put(initAct, initPosition);
            this.initState = initAct;
            this.deviceName = servoName; // Default device name to servo name
        }

        public NormalCRServoBuilder setDeviceName(String Name) {
            this.deviceName = Name;
            return this;
        }

        public NormalCRServoBuilder addAction(Action actionType, double positionInDegrees) {
            actionMap.put(actionType, positionInDegrees);
            return this;
        }

        public NormalCRServoBuilder setSwitcher(Action switch1, Action switch2) {
            if (isSwitcherSet) {
                throw new IllegalStateException("Switcher should only be assigned once.");
            }
            if (!actionMap.containsKey(switch1) || !actionMap.containsKey(switch2)) {
                throw new IllegalArgumentException("Both switcher actions must be defined with addAction() before setting the switcher.");
            }
            this.switcher = SwitcherPair.GetSwitcherPair(switch1, switch2);
            this.isSwitcherSet = true;
            return this;
        }

        public NormalCRServo build() {
            if (!isSwitcherSet) {
                this.switcher = SwitcherPair.GetSwitcherPair(null, null);
            }
            return new NormalCRServo(this);
        }
    }
}
