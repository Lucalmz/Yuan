package com.bear27570.yuan.AdvantageCoreLib.Logging;

import android.provider.Settings;

import java.util.LinkedList;

/**
 * 摄像头缓存器
 */
public class CameraLogger {
    private final int LIST_SIZE = 10;
    private final LinkedList<TimestampedFrame> frames = new LinkedList<>();
    public synchronized void addFrame(TimestampedFrame frame) {
        frames.addLast(frame);
        //控制列表长度
        if (frames.size() > LIST_SIZE) {
            frames.removeFirst();
        }
    }

}
