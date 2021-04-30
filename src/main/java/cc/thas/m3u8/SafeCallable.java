package cc.thas.m3u8;

import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public interface SafeCallable<V> extends Callable<V> {

    V safeCall() throws Exception;

    @Override
    default V call() {
        try {
            return safeCall();
        } catch (Exception e) {
            LoggerFactory.getLogger(SafeCallable.class).error("SafeCallable:", e);
            return null;
        }
    }
}
