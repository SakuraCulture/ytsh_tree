package cn.iocoder.yudao.module.ele.service.traffic;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EleTrafficMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(EleTrafficMetricsCollector.class);

    private static final String TRAFFIC_KEY_PREFIX = "ele:traffic:";
    private static final String RECORD_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "record:";
    private static final String STATS_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "stats:";
    private static final String HOURLY_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "hourly:";
    private static final String RPS_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "api-rps:";
    private static final int RETENTION_DAYS = 3;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 100L;
    private static final long DEGRADATION_CHECK_INTERVAL_MS = 30_000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private volatile boolean redisDegraded = false;
    private volatile long lastDegradationTime = 0L;
    private final ConcurrentHashMap<String, AtomicInteger> fallbackStatsCounter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> fallbackHourlyCounter = new ConcurrentHashMap<>();

    public void recordRequest(String apiCode, String traceId, long requestBytes, long startTime) {
        try {
            String recordKey = traceId != null ? traceId : apiCode + "_" + UUID.randomUUID().toString().substring(0, 8);
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            RealtimeRecord record = new RealtimeRecord();
            record.setApiCode(apiCode);
            record.setTraceId(traceId);
            record.setRequestBytes(requestBytes);
            record.setTimestamp(LocalDateTime.now());
            record.setStartTime(startTime);

            String redisKey = RECORD_KEY_PREFIX + dateStr + ":" + recordKey;
            stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(record), RETENTION_DAYS, TimeUnit.DAYS);

            incrementStats(dateStr, "request_count", 1);
            incrementStats(dateStr, "request_bytes", requestBytes);
            updateMaxStats(dateStr, "max_request_bytes", requestBytes);

            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
            incrementHourlyStats(dateStr, hour, requestBytes, 0, false, false, 0);

            incrementApiRpsCounter(apiCode);

            log.info("[流量采集] apiCode={}, key={}, requestSize={} bytes, date={}", apiCode, recordKey, requestBytes,
                    dateStr);
        } catch (Exception e) {
            log.error("[流量采集] 记录请求失败, apiCode={}, error={}", apiCode, e.getMessage(), e);
        }
    }

    public void recordResponse(String traceId, long responseBytes, boolean success, int durationMs) {
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String key = traceId != null ? traceId : "unknown_" + UUID.randomUUID().toString().substring(0, 8);

            String redisKey = RECORD_KEY_PREFIX + dateStr + ":" + key;
            String recordJson = stringRedisTemplate.opsForValue().get(redisKey);

            RealtimeRecord record;
            boolean isNewRecord = false;

            if (recordJson == null) {
                record = new RealtimeRecord();
                record.setTraceId(traceId);
                record.setTimestamp(LocalDateTime.now());
                isNewRecord = true;
            } else {
                record = JSONUtil.toBean(recordJson, RealtimeRecord.class);
            }

            record.setResponseBytes(responseBytes);
            record.setDurationMs(durationMs);
            record.setSuccess(success);

            stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(record), RETENTION_DAYS, TimeUnit.DAYS);

            incrementStats(dateStr, "response_bytes", responseBytes);
            incrementStats(dateStr, "total_duration_ms", durationMs);
            updateMaxStats(dateStr, "max_response_bytes", responseBytes);

            if (success) {
                incrementStats(dateStr, "success_count", 1);
                log.info("[流量采集] traceId={}, responseSize={} bytes, success={}, duration={}ms, date={}",
                        traceId, responseBytes, success, durationMs, dateStr);
            } else {
                incrementStats(dateStr, "failed_count", 1);
                log.warn("[流量采集] 失败 traceId={}, responseSize={} bytes, duration={}ms, date={}",
                        traceId, responseBytes, durationMs, dateStr);
            }

            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
            if (isNewRecord) {
                incrementHourlyStats(dateStr, hour, 0, responseBytes, success, true, durationMs);
            } else {
                incrementHourlyStats(dateStr, hour, record.getRequestBytes(), responseBytes, success, true, durationMs);
            }
        } catch (Exception e) {
            log.error("[流量采集] 记录响应失败, traceId={}, error={}", traceId, e.getMessage(), e);
        }
    }

    private void incrementStats(String dateStr, String field, long value) {
        String statsKey = STATS_KEY_PREFIX + dateStr;
        String counterKey = statsKey + ":" + field;

        if (redisDegraded) {
            fallbackStatsCounter.computeIfAbsent(counterKey, k -> new AtomicInteger(0)).addAndGet((int) value);
            return;
        }

        try {
            executeWithRetry(() -> {
                stringRedisTemplate.opsForHash().increment(statsKey, field, value);
                stringRedisTemplate.expire(statsKey, RETENTION_DAYS, TimeUnit.DAYS);
                return null;
            }, "incrementStats:" + field);
        } catch (Exception e) {
            log.warn("[流量采集] Redis操作失败，启用降级，field={}, error={}", field, e.getMessage());
            markRedisDegraded();
            fallbackStatsCounter.computeIfAbsent(counterKey, k -> new AtomicInteger(0)).addAndGet((int) value);
        }
    }

    private void updateMaxStats(String dateStr, String field, long value) {
        String statsKey = STATS_KEY_PREFIX + dateStr;

        if (redisDegraded) {
            return;
        }

        try {
            executeWithRetry(() -> {
                String currentMaxStr = (String) stringRedisTemplate.opsForHash().get(statsKey, field);
                long currentMax = currentMaxStr != null ? Long.parseLong(currentMaxStr) : 0L;
                if (value > currentMax) {
                    stringRedisTemplate.opsForHash().put(statsKey, field, String.valueOf(value));
                    stringRedisTemplate.expire(statsKey, RETENTION_DAYS, TimeUnit.DAYS);
                }
                return null;
            }, "updateMaxStats:" + field);
        } catch (Exception e) {
            log.warn("[流量采集] Redis操作失败，启用降级，field={}, error={}", field, e.getMessage());
            markRedisDegraded();
        }
    }

    private void incrementHourlyStats(String dateStr, String hour, long requestBytes, long responseBytes,
            boolean success, boolean isComplete, int durationMs) {
        if (!isComplete) {
            return;
        }

        String hourlyKey = HOURLY_KEY_PREFIX + dateStr + ":" + hour;

        if (redisDegraded) {
            String counterKey = hourlyKey;
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("request_count", k -> new AtomicInteger(0)).incrementAndGet();
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("request_bytes", k -> new AtomicInteger(0)).addAndGet((int) requestBytes);
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("response_bytes", k -> new AtomicInteger(0)).addAndGet((int) responseBytes);
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("total_duration_ms", k -> new AtomicInteger(0)).addAndGet(durationMs);
            if (success) {
                fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent("success_count", k -> new AtomicInteger(0)).incrementAndGet();
            } else {
                fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent("failed_count", k -> new AtomicInteger(0)).incrementAndGet();
            }
            return;
        }

        try {
            executeWithRetry(() -> {
                stringRedisTemplate.opsForHash().increment(hourlyKey, "request_count", 1);
                stringRedisTemplate.opsForHash().increment(hourlyKey, "request_bytes", requestBytes);
                stringRedisTemplate.opsForHash().increment(hourlyKey, "response_bytes", responseBytes);
                stringRedisTemplate.opsForHash().increment(hourlyKey, "total_duration_ms", durationMs);

                if (success) {
                    stringRedisTemplate.opsForHash().increment(hourlyKey, "success_count", 1);
                } else {
                    stringRedisTemplate.opsForHash().increment(hourlyKey, "failed_count", 1);
                }
                stringRedisTemplate.expire(hourlyKey, RETENTION_DAYS, TimeUnit.DAYS);
                return null;
            }, "incrementHourlyStats:hour" + hour);
        } catch (Exception e) {
            log.warn("[流量采集] Redis操作失败，启用降级，hour={}, error={}", hour, e.getMessage());
            markRedisDegraded();

            String counterKey = hourlyKey;
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("request_count", k -> new AtomicInteger(0)).incrementAndGet();
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("request_bytes", k -> new AtomicInteger(0)).addAndGet((int) requestBytes);
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("response_bytes", k -> new AtomicInteger(0)).addAndGet((int) responseBytes);
            fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent("total_duration_ms", k -> new AtomicInteger(0)).addAndGet(durationMs);
            if (success) {
                fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent("success_count", k -> new AtomicInteger(0)).incrementAndGet();
            } else {
                fallbackHourlyCounter.computeIfAbsent(counterKey, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent("failed_count", k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                attempts++;

                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    break;
                }

                long delay = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempts - 1);
                log.debug("[流量采集] 第{}次重试，操作={}, 延迟={}ms, error={}", attempts, operationName, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }

        throw lastException;
    }

    private void markRedisDegraded() {
        long now = System.currentTimeMillis();
        if (!redisDegraded || (now - lastDegradationTime) > DEGRADATION_CHECK_INTERVAL_MS) {
            this.redisDegraded = true;
            this.lastDegradationTime = now;
            log.warn("[流量采集] Redis服务进入降级状态，使用内存计数器");
        }
    }

    public void checkRedisRecovery() {
        if (redisDegraded && (System.currentTimeMillis() - lastDegradationTime) > DEGRADATION_CHECK_INTERVAL_MS) {
            try {
                stringRedisTemplate.getConnectionFactory().getConnection().ping();
                if (redisDegraded) {
                    log.info("[流量采集] Redis服务已恢复，退出降级状态");
                    redisDegraded = false;
                    flushFallbackData();
                }
            } catch (Exception e) {
                lastDegradationTime = System.currentTimeMillis();
            }
        }
    }

    private void flushFallbackData() {
        if (fallbackStatsCounter.isEmpty() && fallbackHourlyCounter.isEmpty()) {
            return;
        }

        try {
            log.info("[流量采集] 开始同步降级数据到Redis，stats数量={}, hourly数量={}",
                    fallbackStatsCounter.size(), fallbackHourlyCounter.size());

            for (Map.Entry<String, AtomicInteger> entry : fallbackStatsCounter.entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length >= 2) {
                    String statsKey = parts[0] + ":" + parts[1];
                    String field = parts[2];
                    int value = entry.getValue().get();
                    if (value > 0) {
                        stringRedisTemplate.opsForHash().increment(statsKey, field, value);
                    }
                }
            }

            for (Map.Entry<String, ConcurrentHashMap<String, AtomicInteger>> entry : fallbackHourlyCounter.entrySet()) {
                String hourlyKey = entry.getKey();
                for (Map.Entry<String, AtomicInteger> fieldEntry : entry.getValue().entrySet()) {
                    int value = fieldEntry.getValue().get();
                    if (value > 0) {
                        stringRedisTemplate.opsForHash().increment(hourlyKey, fieldEntry.getKey(), value);
                    }
                }
                stringRedisTemplate.expire(hourlyKey, RETENTION_DAYS, TimeUnit.DAYS);
            }

            fallbackStatsCounter.clear();
            fallbackHourlyCounter.clear();

            log.info("[流量采集] 降级数据同步完成");
        } catch (Exception e) {
            log.error("[流量采集] 同步降级数据失败, error={}", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    public RealtimeRecord getRecord(String traceId) {
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String redisKey = RECORD_KEY_PREFIX + dateStr + ":" + traceId;
            String recordJson = stringRedisTemplate.opsForValue().get(redisKey);

            if (recordJson != null) {
                return JSONUtil.toBean(recordJson, RealtimeRecord.class);
            }
            return null;
        } catch (Exception e) {
            log.error("[流量采集] 获取记录失败, traceId={}, error={}", traceId, e.getMessage());
            return null;
        }
    }

    public long getRequestCount() {
        return getRequestCount(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getRequestCount(String dateStr) {
        return getStatsField(dateStr, "request_count");
    }

    public long getTotalRequestBytes() {
        return getTotalRequestBytes(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getTotalRequestBytes(String dateStr) {
        return getStatsField(dateStr, "request_bytes");
    }

    public long getTotalResponseBytes() {
        return getTotalResponseBytes(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getTotalResponseBytes(String dateStr) {
        return getStatsField(dateStr, "response_bytes");
    }

    public long getSuccessCount() {
        return getSuccessCount(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getSuccessCount(String dateStr) {
        return getStatsField(dateStr, "success_count");
    }

    public long getFailedCount() {
        return getFailedCount(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getFailedCount(String dateStr) {
        return getStatsField(dateStr, "failed_count");
    }

    public long getTotalDurationMs() {
        return getTotalDurationMs(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getTotalDurationMs(String dateStr) {
        return getStatsField(dateStr, "total_duration_ms");
    }

    public long getMaxRequestBytes() {
        return getMaxRequestBytes(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getMaxRequestBytes(String dateStr) {
        return getStatsField(dateStr, "max_request_bytes");
    }

    public long getMaxResponseBytes() {
        return getMaxResponseBytes(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public long getMaxResponseBytes(String dateStr) {
        return getStatsField(dateStr, "max_response_bytes");
    }

    private long getStatsField(String field) {
        return getStatsField(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), field);
    }

    private long getStatsField(String dateStr, String field) {
        checkRedisRecovery();

        try {
            String statsKey = STATS_KEY_PREFIX + dateStr;
            Object value = stringRedisTemplate.opsForHash().get(statsKey, field);
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (Exception e) {
            log.error("[流量采集] 获取统计字段失败, dateStr={}, field={}, error={}", dateStr, field, e.getMessage());

            String counterKey = STATS_KEY_PREFIX + dateStr + ":" + field;
            AtomicInteger fallbackValue = fallbackStatsCounter.get(counterKey);
            return fallbackValue != null ? fallbackValue.get() : 0L;
        }
    }

    public int getRecordCount() {
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String pattern = RECORD_KEY_PREFIX + dateStr + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("[流量采集] 获取记录数量失败, error={}", e.getMessage());
            return 0;
        }
    }

    public Map<String, HourlyStats> getHourlyStatsMap() {
        return getHourlyStatsMapByDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public Map<String, HourlyStats> getHourlyStatsMapByDate(String dateStr) {
        Map<String, HourlyStats> result = new LinkedHashMap<>();
        try {
            for (int hour = 0; hour < 24; hour++) {
                String hourStr = String.format("%02d", hour);
                String hourlyKey = HOURLY_KEY_PREFIX + dateStr + ":" + hourStr;
                Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(hourlyKey);

                if (!entries.isEmpty()) {
                    HourlyStats stats = new HourlyStats();
                    stats.setRequestCount(parseLong(entries.get("request_count")));
                    stats.setTotalRequestBytes(parseLong(entries.get("request_bytes")));
                    stats.setTotalResponseBytes(parseLong(entries.get("response_bytes")));
                    stats.setSuccessCount(parseLong(entries.get("success_count")));
                    stats.setFailedCount(parseLong(entries.get("failed_count")));
                    stats.setTotalDurationMs(parseLong(entries.get("total_duration_ms")));
                    result.put(hourStr, stats);
                }
            }
        } catch (Exception e) {
            log.error("[流量采集] 获取小时统计失败, dateStr={}, error={}", dateStr, e.getMessage());
        }
        return result;
    }

    private long parseLong(Object val) {
        if (val == null)
            return 0L;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void resetForNewDay() {
        try {
            String threeDaysAgo = LocalDate.now().minusDays(3).format(DateTimeFormatter.BASIC_ISO_DATE);

            String threeDaysAgoStatsKey = STATS_KEY_PREFIX + threeDaysAgo;
            stringRedisTemplate.delete(threeDaysAgoStatsKey);

            String pattern = RECORD_KEY_PREFIX + threeDaysAgo + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }

            String hourlyPattern = HOURLY_KEY_PREFIX + threeDaysAgo + ":*";
            Set<String> hourlyKeys = stringRedisTemplate.keys(hourlyPattern);
            if (hourlyKeys != null && !hourlyKeys.isEmpty()) {
                stringRedisTemplate.delete(hourlyKeys);
            }

            log.info("[流量采集] 清理3天前数据完成, date={}", threeDaysAgo);
        } catch (Exception e) {
            log.error("[流量采集] 清理过期数据失败, error={}", e.getMessage());
        }
    }

    public void resetTodayOnly() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            String todayStatsKey = STATS_KEY_PREFIX + today;
            stringRedisTemplate.delete(todayStatsKey);

            String pattern = RECORD_KEY_PREFIX + today + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }

            String hourlyPattern = HOURLY_KEY_PREFIX + today + ":*";
            Set<String> hourlyKeys = stringRedisTemplate.keys(hourlyPattern);
            if (hourlyKeys != null && !hourlyKeys.isEmpty()) {
                stringRedisTemplate.delete(hourlyKeys);
            }

            log.info("[流量采集] 重置今日数据完成, date={}", today);
        } catch (Exception e) {
            log.error("[流量采集] 重置今日数据失败, error={}", e.getMessage());
        }
    }

    public List<String> getAvailableDates() {
        List<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < RETENTION_DAYS; i++) {
            dates.add(today.minusDays(i).format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        return dates;
    }

    private void incrementApiRpsCounter(String apiCode) {
        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + currentSecond;
            stringRedisTemplate.opsForValue().increment(rpsKey, 1);
            stringRedisTemplate.expire(rpsKey, 120, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[流量采集] 更新RPS计数器失败, apiCode={}, error={}", apiCode, e.getMessage());
        }
    }

    public Map<String, Integer> getApiRpsForSecond(String apiCode, long timestampSecond) {
        Map<String, Integer> result = new HashMap<>();
        try {
            String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + timestampSecond;
            String value = stringRedisTemplate.opsForValue().get(rpsKey);
            result.put("current", value != null ? Integer.parseInt(value) : 0);
        } catch (Exception e) {
            log.warn("[流量采集] 获取RPS失败, apiCode={}, error={}", apiCode, e.getMessage());
            result.put("current", 0);
        }
        return result;
    }

    public int getAvgRpsForWindow(String apiCode, int windowSeconds) {
        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            int total = 0;
            for (int i = 0; i < windowSeconds; i++) {
                String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + (currentSecond - i);
                String value = stringRedisTemplate.opsForValue().get(rpsKey);
                if (value != null) {
                    total += Integer.parseInt(value);
                }
            }
            return total / windowSeconds;
        } catch (Exception e) {
            log.warn("[流量采集] 获取平均RPS失败, apiCode={}, error={}", apiCode, e.getMessage());
            return 0;
        }
    }

    public int getMaxRpsForWindow(String apiCode, int windowSeconds) {
        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            int max = 0;
            for (int i = 0; i < windowSeconds; i++) {
                String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + (currentSecond - i);
                String value = stringRedisTemplate.opsForValue().get(rpsKey);
                if (value != null) {
                    int count = Integer.parseInt(value);
                    if (count > max) {
                        max = count;
                    }
                }
            }
            return max;
        } catch (Exception e) {
            log.warn("[流量采集] 获取峰值RPS失败, apiCode={}, error={}", apiCode, e.getMessage());
            return 0;
        }
    }

    public List<Integer> getRpsHistory(String apiCode, int seconds) {
        List<Integer> history = new ArrayList<>();
        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            for (int i = seconds - 1; i >= 0; i--) {
                String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + (currentSecond - i);
                String value = stringRedisTemplate.opsForValue().get(rpsKey);
                history.add(value != null ? Integer.parseInt(value) : 0);
            }
        } catch (Exception e) {
            log.warn("[流量采集] 获取RPS历史失败, apiCode={}, error={}", apiCode, e.getMessage());
            for (int i = history.size(); i < seconds; i++) {
                history.add(0);
            }
        }
        return history;
    }

    public static class RealtimeRecord {
        private String apiCode;
        private String traceId;
        private long requestBytes;
        private long responseBytes;
        private int durationMs;
        private boolean success;
        private LocalDateTime timestamp;
        private long startTime;

        public String getApiCode() {
            return apiCode;
        }

        public void setApiCode(String apiCode) {
            this.apiCode = apiCode;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public long getRequestBytes() {
            return requestBytes;
        }

        public void setRequestBytes(long requestBytes) {
            this.requestBytes = requestBytes;
        }

        public long getResponseBytes() {
            return responseBytes;
        }

        public void setResponseBytes(long responseBytes) {
            this.responseBytes = responseBytes;
        }

        public int getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(int durationMs) {
            this.durationMs = durationMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
    }

    public static class HourlyStats {
        private long requestCount = 0;
        private long totalRequestBytes = 0;
        private long totalResponseBytes = 0;
        private long successCount = 0;
        private long failedCount = 0;
        private long totalDurationMs = 0;

        public long getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(long requestCount) {
            this.requestCount = requestCount;
        }

        public long getTotalRequestBytes() {
            return totalRequestBytes;
        }

        public void setTotalRequestBytes(long totalRequestBytes) {
            this.totalRequestBytes = totalRequestBytes;
        }

        public long getTotalResponseBytes() {
            return totalResponseBytes;
        }

        public void setTotalResponseBytes(long totalResponseBytes) {
            this.totalResponseBytes = totalResponseBytes;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(long successCount) {
            this.successCount = successCount;
        }

        public long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(long failedCount) {
            this.failedCount = failedCount;
        }

        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        public void setTotalDurationMs(long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
        }

        public double getAvgDurationMs() {
            return requestCount > 0 ? (double) totalDurationMs / requestCount : 0.0;
        }
    }
}
