package com.bear27570.yuan.BotFactory.Interface;

public interface LockableGroup {
    /**
     * 全部上锁方法
     */
    public void lockAllSubsystems();

    /**
     * 全部释放锁方法
     */
    public void unLockAllSubsystems();
}
