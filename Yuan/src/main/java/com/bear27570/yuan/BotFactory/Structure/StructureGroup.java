package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Interface.LockableGroup;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;

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
public class StructureGroup implements LockableGroup {
    private final ArrayList<RunnableStructUnit> RSU;
    private final ArrayList<StructureLink> Structures;
    private final int StructureNum;
    private final int RSUNum;
    public ReentrantLock lock = new ReentrantLock();
    private StructureGroup(@NonNull StructureBuilder Builder){
        this.Structures=Builder.Structures;
        this.RSU=Builder.RSU;
        StructureNum=Structures.size();
        RSUNum = RSU.size();
    }
    /**
     * 将子系统全部上锁
     */
    @Override
    public void lockAllSubsystems(){
        for (int i = 0; i < RSUNum; i++) {
            RSU.get(i).lock();
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
        for (int i = 0; i < RSUNum; i++) {
            RSU.get(i).lock();
        }
        for(int i = 0; i < StructureNum; i++){
            Structures.get(i).lock.unlock();
            Structures.get(i).unLockAllSubsystems();
        }
    }
    public static class StructureBuilder {
        private ArrayList<RunnableStructUnit> RSU;
        private ArrayList<StructureLink> Structures;

        /**
         * 空参构造，无特殊必须的参数
         */
        public StructureBuilder() {
        }

        /**
         * 添加一个活动单元
         *
         * @param newRSU 这个结构中的一个活动单元
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(RunnableStructUnit newRSU) {
            this.RSU.add(newRSU);
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
         * 构建并返回一个 StructureGroup实例。
         *
         * @return 构建好的 StructureGroup 对象
         */
        public StructureGroup build() {
            return new StructureGroup(this);
        }
    }
}
