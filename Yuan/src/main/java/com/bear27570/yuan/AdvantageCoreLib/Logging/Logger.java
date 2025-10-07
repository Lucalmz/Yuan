package com.bear27570.yuan.AdvantageCoreLib.Logging;

import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.concurrent.ThreadSafe;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * 全局单例日志记录器.
 * 负责将所有硬件数据以高效的二进制格式写入文件.
 * 线程安全.
 * [1 byte  - Record Type]   0=double, 1=boolean, 2=String, 3=double[] ...
 * [8 bytes - Timestamp (long)]  System.nanoTime()
 * [n bytes - Field Name (UTF String)]  e.g., "leftMotor/commandedPower"
 * [m bytes - Payload (Data)]  具体的 double, boolean, String 等
 */
@ThreadSafe
public class Logger {

    private static Logger INSTANCE = null;
    private final Object lock = new Object();
    private DataOutputStream logStream;
    private final LongSupplier timestampSupplier;

    // 全局日志开关
    private final boolean isLoggingEnabled;

    private Logger(File logFile, boolean enable, LongSupplier timestampSupplier) throws IOException {
        this.isLoggingEnabled = enable;
        this.timestampSupplier = timestampSupplier;
        if (!isLoggingEnabled) return;

        // 核心性能优化: 使用BufferedOutputStream来减少实际的磁盘写入次数
        this.logStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFile)));
    }

    /**
     * 初始化Logger. 必须在init()方法中首先调用!
     * @param enableLogging 本次运行是否启用日志记录.
     * @param timestampSupplier 提供高精度时间戳的函数, 例如 System::nanoTime.
     */
    public static synchronized void initialize(boolean enableLogging, LongSupplier timestampSupplier) {
        if (INSTANCE != null) {
            System.out.println("Logger is already initialized. Skipping.");
            return;
        }

        try {
            // 如果禁用日志，创建一个什么都不做的“哑”实例
            if (!enableLogging) {
                INSTANCE = new Logger(null, false, null);
                return;
            }

            // 创建日志文件
            File logDir = new File("/sdcard/FIRST/AdvantageLogs/");
            logDir.mkdirs(); // 确保文件夹存在
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File logFile = new File(logDir, "log_" + timestamp + ".advlog");

            INSTANCE = new Logger(logFile, true, timestampSupplier);
        } catch (IOException e) {
            // 如果初始化失败，这是个严重问题，直接抛出运行时异常
            throw new RuntimeException("Failed to initialize logger!", e);
        }
    }

    /**
     * 获取Logger的唯一实例.
     */
    public static Logger getINSTANCE() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Logger.getInstance() called before initialize(). This is not allowed.");
        }
        return INSTANCE;
    }

    public void logDouble(String key, double value) {
        if (!isLoggingEnabled) return;
        synchronized (lock) {
            try {
                logStream.writeByte(0); // Type 0: double
                logStream.writeLong(timestampSupplier.getAsLong());
                logStream.writeUTF(key);
                logStream.writeDouble(value);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void logBoolean(String key, boolean value) {
        if (!isLoggingEnabled) return;
        synchronized (lock) {
            try {
                logStream.writeByte(1); // Type 1: boolean
                logStream.writeLong(timestampSupplier.getAsLong());
                logStream.writeUTF(key);
                logStream.writeBoolean(value);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void logString(String key, String value) {
        if (!isLoggingEnabled) return;
        synchronized (lock) {
            try {
                logStream.writeByte(2); // Type 2: String
                logStream.writeLong(timestampSupplier.getAsLong());
                logStream.writeUTF(key);
                logStream.writeUTF(value);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void logImage(String key,long timestamp, byte[] value) {
        if (!isLoggingEnabled) return;
        synchronized (lock) {
            try {
                logStream.writeByte(3); // Type 3: Image
                logStream.writeLong(timestamp);
                logStream.writeUTF(key);
                logStream.writeInt(value.length);
                logStream.write(value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭logger，释放文件资源。必须在OpMode的stop()中调用！
     */
    public void close() {
        if (!isLoggingEnabled || logStream == null) return;
        try {
            synchronized (lock) {
                logStream.flush(); // 确保缓冲区的所有数据都被写入
                logStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            INSTANCE = null; // 允许下一个OpMode重新初始化
        }
    }
}