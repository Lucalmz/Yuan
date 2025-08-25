package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.LockableUnit;
import com.bear27570.yuan.BotFactory.Motor.MotorEx;
import com.bear27570.yuan.BotFactory.Servo.CRServoFactory;
import com.bear27570.yuan.BotFactory.Servo.ServoEx;

import java.util.ArrayList;
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
public class StructureGroup implements LockableUnit {
    private final ArrayList<ServoEx> Servos;
    private final ArrayList<MotorEx> Motors;
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
    /**
     * 释放所有子系统的锁
     */
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
        private ArrayList<ServoEx> Servos;
        private ArrayList<MotorEx> Motors;
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
        public StructureBuilder add(ServoEx newServo) {
            this.Servos.add(newServo);
            return this;
        }

        /**
         * 添加一个电机
         *
         * @param newMotor 这个结构中的一个电机
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(MotorEx newMotor) {
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
