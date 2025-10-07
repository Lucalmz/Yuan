package com.bear27570.yuan.AdvantageCoreLib.Logging;

public class TimestampedFrame {
    public final long timestamp;
    public final byte[] data;
    public TimestampedFrame(long timestamp, byte[] data) {
        this.timestamp = timestamp;
        this.data = data;
    }
}
