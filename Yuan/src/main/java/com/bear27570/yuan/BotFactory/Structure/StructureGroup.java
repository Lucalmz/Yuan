package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.CRServoActPair;
import com.bear27570.yuan.BotFactory.LockableUnit;
import com.bear27570.yuan.BotFactory.Motor.MotorFactory;
import com.bear27570.yuan.BotFactory.MotorActPair;
import com.bear27570.yuan.BotFactory.Servo.CRServoFactory;
import com.bear27570.yuan.BotFactory.Servo.ServoFactory;
import com.bear27570.yuan.BotFactory.ServoActPair;
import com.bear27570.yuan.BotFactory.StructureActionPair;
import com.bear27570.yuan.BotFactory.SwitcherPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class StructureGroup implements LockableUnit {
    private final ArrayList<ServoFactory> Servos;
    private final ArrayList<MotorFactory> Motors;
    private final ArrayList<CRServoFactory> CRServos;
    private final ArrayList<StructureLink> Structures;
    private final int ServoNum;
    private final int CRServoNum;
    private final int StructureNum;
    private final int MotorNum;
    public ReentrantLock lock = new ReentrantLock();
    private StructureGroup(@NonNull StructureBuilder Builder){
        this.Servos=Builder.Servos;
        this.CRServos=Builder.CRServos;
        this.Structures=Builder.Structures;
        this.Motors=Builder.Motors;
        ServoNum=Servos.size();
        CRServoNum=CRServos.size();
        MotorNum=Motors.size();
        StructureNum=Structures.size();
    }
    /**
     * 将子系统全部上锁
     */
    @Override
    public void lockAllSubsystems(){
        for (int i = 0; i < ServoNum; i++) {
            Servos.get(i).lock.lock();
        }
        for (int i = 0; i < MotorNum; i++) {
            Motors.get(i).lock.lock();
        }
        for (int i = 0; i < CRServoNum; i++) {
            CRServos.get(i).lock.lock();
        }
        for(int i = 0; i < StructureNum; i++){
            Structures.get(i).lock.lock();
            Structures.get(i).lockAllSubsystems();
        }
    }
    public void unLockAllSubsystems(){
        for (int i = 0; i < ServoNum; i++) {
            Servos.get(i).lock.unlock();
        }
        for (int i = 0; i < MotorNum; i++) {
            Motors.get(i).lock.unlock();
        }
        for (int i = 0; i < CRServoNum; i++) {
            CRServos.get(i).lock.unlock();
        }
        for(int i = 0; i < StructureNum; i++){
            Structures.get(i).lock.unlock();
            Structures.get(i).unLockAllSubsystems();
        }
    }
    public static class StructureBuilder {
        private ArrayList<ServoFactory> Servos;
        private ArrayList<MotorFactory> Motors;
        private ArrayList<CRServoFactory> CRServos;
        private ArrayList<StructureLink> Structures;

        /**
         * 空参构造，无特殊必须的参数
         */
        public StructureBuilder() {
        }

        /**
         * 添加一个舵机
         *
         * @param newServo 这个结构中的一个舵机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(ServoFactory newServo) {
            this.Servos.add(newServo);
            return this;
        }

        /**
         * 添加一个电机
         *
         * @param newMotor 这个结构中的一个电机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(MotorFactory newMotor) {
            this.Motors.add(newMotor);
            return this;
        }

        /**
         * 添加一个CR舵机
         *
         * @param newCRServo 这个结构中的一个CR舵机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(CRServoFactory newCRServo) {
            this.CRServos.add(newCRServo);
            return this;
        }
        /**
         * 添加一个结构链
         *
         * @param newStructure 这个结构中的一个结构链
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(StructureLink newStructure) {
            this.Structures.add(newStructure);
            return this;
        }
        /**
         * 构建并返回一个 StructureLink 实例。
         *
         * @return 构建好的 StructureLink 对象
         */
        public StructureGroup build() {
            return new StructureGroup(this);
        }
    }
}
