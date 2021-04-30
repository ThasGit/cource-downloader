package cc.thas.m3u8;

import org.slf4j.LoggerFactory;

public interface SafeRunnable extends Runnable {

    void safeRun() throws Exception;

    @Override
    default void run() {
        try {
            safeRun();
        } catch (Exception e) {
            LoggerFactory.getLogger(SafeRunnable.class).error("SafeRunnable:", e);
        }
    }
}
