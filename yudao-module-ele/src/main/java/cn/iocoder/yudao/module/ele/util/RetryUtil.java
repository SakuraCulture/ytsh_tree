package cn.iocoder.yudao.module.ele.util;

import cn.iocoder.yudao.module.ele.exception.EleOrderSyncException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class RetryUtil {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY = 100;
    private static final long DEFAULT_MAX_DELAY = 5000;
    private static final double DEFAULT_MULTIPLIER = 2.0;

    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY, DEFAULT_MAX_DELAY);
    }

    public static <T> T executeWithRetry(Supplier<T> operation, String operationName,
                                       int maxRetries, long initialDelay, long maxDelay) {
        int attempt = 0;
        long currentDelay = initialDelay;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                log.debug("【重试】执行 {}，第 {} 次尝试", operationName, attempt);
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                boolean isRetryable = isRetryableException(e);

                if (!isRetryable || attempt >= maxRetries) {
                    if (e instanceof EleOrderSyncException) {
                        log.error("【重试】{} 执行失败，Redis锁异常，已达最大重试次数 {} 次", operationName, maxRetries, e);
                        throw (EleOrderSyncException) e;
                    }
                    log.error("【重试】{} 执行失败，已达最大重试次数 {} 次，不再重试", operationName, maxRetries, e);
                    throw new RuntimeException(operationName + " 执行失败: " + e.getMessage(), e);
                }

                log.warn("【重试】{} 第 {} 次执行失败，{}ms 后重试: {}",
                        operationName, attempt, currentDelay, e.getMessage());
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(operationName + " 被中断", ie);
                }
                currentDelay = Math.min((long) (currentDelay * DEFAULT_MULTIPLIER), maxDelay);
            }
        }

        throw new RuntimeException(operationName + " 执行失败", lastException);
    }

    public static void executeWithRetry(Runnable operation, String operationName) {
        executeWithRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY, DEFAULT_MAX_DELAY);
    }

    public static void executeWithRetry(Runnable operation, String operationName,
                                     int maxRetries, long initialDelay, long maxDelay) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, operationName, maxRetries, initialDelay, maxDelay);
    }

    private static boolean isRetryableException(Exception e) {
        if (e instanceof EleOrderSyncException) {
            return true;
        }
        String message = e.getMessage();
        if (message == null) {
            return true;
        }
        String lowerMessage = message.toLowerCase();

        return lowerMessage.contains("connection")
                || lowerMessage.contains("timeout")
                || lowerMessage.contains("socket")
                || lowerMessage.contains("refused")
                || lowerMessage.contains("unavailable")
                || lowerMessage.contains("network");
    }
}