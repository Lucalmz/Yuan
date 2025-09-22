package com.bear27570.yuan.BotFactory.Structure;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Interface.Lockable;
import com.bear27570.yuan.BotFactory.Interface.RunnableStructUnit;
import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;

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
@ThreadSafe
public class StructureGroup{
    private final ArrayList<Lockable> Unit;
    private final int UnitNum;
    public ReentrantLock lock = new ReentrantLock();
    private StructureGroup(@NonNull StructureBuilder Builder){
        this.Unit = Builder.Unit;
        this.UnitNum = Builder.Unit.size();
    }
    /**
     * 将子系统全部上锁
     */
    public boolean tryLock(){
        ArrayList<Lockable> successfulList = new ArrayList<>();
        for (int i = 0; i < UnitNum; i++) {
            if(!Unit.get(i).tryLock()) {
                for (int m = 0; m < successfulList.size(); m++) {
                    successfulList.get(m).unlock();
                }
                return false;
            }
            successfulList.add(Unit.get(i));
        }
        if(!lock.tryLock()){
            return false;
        }
        return true;
    }
    /**
     * 释放所有子系统的锁
     */
    public void unlock(){
        for (int i = 0; i < UnitNum; i++) {
            Unit.get(i).unlock();
        }
    }
    public static class StructureBuilder {
        private ArrayList<Lockable> Unit;

        /**
         * 空参构造，无特殊必须的参数
         */
        public StructureBuilder() {
        }

        /**
         * 添加一个活动单元
         *
         * @param newUnit 这个结构中的一个活动单元
         * @return 当前Builder实例，实现链式调用
         */
        public StructureBuilder add(Lockable newUnit) {
            this.Unit.add(newUnit);
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
