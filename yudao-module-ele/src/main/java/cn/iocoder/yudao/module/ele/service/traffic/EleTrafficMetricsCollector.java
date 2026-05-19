package cn.iocoder.yudao.module.ele.service.traffic;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EleTrafficMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(EleTrafficMetricsCollector.class);

    private static final String TRAFFIC_KEY_PREFIX = "ele:traffic:";
    private static final String RECORD_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "record:";
    private static final String STATS_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "stats:";
    private static final String HOURLY_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "hourly:";
    private static final String RPS_KEY_PREFIX = TRAFFIC_KEY_PREFIX + "api-rps:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private EleTrafficMetricsProperties properties;

    private volatile boolean redisDegraded = false;
    private volatile long lastDegradationTime = 0L;
    private final ConcurrentHashMap<String, AtomicInteger> fallbackStatsCounter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> fallbackHourlyCounter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> aggregateStatsCounter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>> aggregateHourlyCounter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> aggregateRpsCounter = new ConcurrentHashMap<>();

    public void recordRequest(String apiCode, String traceId, long requestBytes, long startTime) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            incrementStats(dateStr, "request_count", 1);
            incrementStats(dateStr, "request_bytes", requestBytes);
            updateMaxStats(dateStr, "max_request_bytes", requestBytes);
            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
            aggregateHourlyStats(dateStr, hour, requestBytes, 0, true, false, 0);
            incrementApiRpsCounter(apiCode);
        } catch (Exception e) {
            log.debug("[流量采集] 请求采集降级, apiCode={}, error={}", apiCode, e.getMessage());
        }
    }

    public void recordResponse(String traceId, long responseBytes, boolean success, int durationMs) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            incrementStats(dateStr, "response_bytes", responseBytes);
            incrementStats(dateStr, "total_duration_ms", durationMs);
            updateMaxStats(dateStr, "max_response_bytes", responseBytes);
            incrementStats(dateStr, success ? "success_count" : "failed_count", 1);
            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
            aggregateHourlyStats(dateStr, hour, 0, responseBytes, false, success, durationMs);
        } catch (Exception e) {
            log.debug("[流量采集] 响应采集降级, traceId={}, error={}", traceId, e.getMessage());
        }
    }

    private void incrementStats(String dateStr, String field, long value) {
        aggregateStatsCounter.computeIfAbsent(dateStr + ":" + field, k -> new AtomicLong(0)).addAndGet(value);
    }

    private void updateMaxStats(String dateStr, String field, long value) {
        aggregateStatsCounter.compute(dateStr + ":" + field, (key, old) -> {
            AtomicLong counter = old == null ? new AtomicLong(0) : old;
            counter.accumulateAndGet(value, Math::max);
            return counter;
        });
    }

    private void aggregateHourlyStats(String dateStr, String hour, long requestBytes, long responseBytes,
            boolean requestEvent, boolean success, int durationMs) {
        String hourlyKey = HOURLY_KEY_PREFIX + dateStr + ":" + hour;
        ConcurrentHashMap<String, AtomicLong> fields = aggregateHourlyCounter.computeIfAbsent(hourlyKey,
                key -> new ConcurrentHashMap<>());
        if (requestEvent) {
            fields.computeIfAbsent("request_count", key -> new AtomicLong(0)).incrementAndGet();
        }
        if (requestBytes > 0) {
            fields.computeIfAbsent("request_bytes", key -> new AtomicLong(0)).addAndGet(requestBytes);
        }
        if (responseBytes > 0) {
            fields.computeIfAbsent("response_bytes", key -> new AtomicLong(0)).addAndGet(responseBytes);
        }
        if (durationMs > 0) {
            fields.computeIfAbsent("total_duration_ms", key -> new AtomicLong(0)).addAndGet(durationMs);
            fields.computeIfAbsent(success ? "success_count" : "failed_count", key -> new AtomicLong(0)).incrementAndGet();
        }
    }

    private void incrementHourlyStats(String dateStr, String hour, long requestBytes, long responseBytes,
            boolean success, boolean isComplete, int durationMs) {
        aggregateHourlyStats(dateStr, hour, requestBytes, responseBytes, !isComplete, success, durationMs);
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) throws Exception {
        return operation.execute();
    }

    private void markRedisDegraded() {
        long now = System.currentTimeMillis();
        if (!redisDegraded || (now - lastDegradationTime) > properties.getDegradationCheckIntervalMs()) {
            this.redisDegraded = true;
            this.lastDegradationTime = now;
            log.warn("[流量采集] Redis服务进入降级状态，采集暂存内存，不影响主流程");
        }
    }

    @Scheduled(fixedDelayString = "${ele.traffic.metrics.flush-interval-ms:5000}")
    public void flushAggregatedMetrics() {
        if (!properties.isEnabled()) {
            aggregateStatsCounter.clear();
            aggregateHourlyCounter.clear();
            aggregateRpsCounter.clear();
            return;
        }
        if (redisDegraded && (System.currentTimeMillis() - lastDegradationTime) <= properties.getDegradationCheckIntervalMs()) {
            return;
        }
        try {
            if (redisDegraded) {
                stringRedisTemplate.getConnectionFactory().getConnection().ping();
                redisDegraded = false;
                log.info("[流量采集] Redis服务已恢复，恢复批量刷写");
            }
            flushStatsAggregate();
            flushHourlyAggregate();
            flushRpsAggregate();
        } catch (Exception e) {
            markRedisDegraded();
            log.warn("[流量采集] Redis批量刷写失败，已降级，不影响主流程，error={}", e.getMessage());
        }
    }

    private void flushStatsAggregate() {
        aggregateStatsCounter.forEach((counterKey, counter) -> {
            long value = counter.getAndSet(0);
            if (value <= 0) {
                return;
            }
            String[] parts = counterKey.split(":", 2);
            if (parts.length != 2) {
                counter.addAndGet(value);
                return;
            }
            String statsKey = STATS_KEY_PREFIX + parts[0];
            String field = parts[1];
            if (field.startsWith("max_")) {
                Object current = stringRedisTemplate.opsForHash().get(statsKey, field);
                long currentMax = current != null ? Long.parseLong(current.toString()) : 0L;
                if (value > currentMax) {
                    stringRedisTemplate.opsForHash().put(statsKey, field, String.valueOf(value));
                }
            } else {
                stringRedisTemplate.opsForHash().increment(statsKey, field, value);
            }
            stringRedisTemplate.expire(statsKey, properties.getRetentionDays(), TimeUnit.DAYS);
        });
    }

    private void flushHourlyAggregate() {
        aggregateHourlyCounter.forEach((hourlyKey, fields) -> {
            fields.forEach((field, counter) -> {
                long value = counter.getAndSet(0);
                if (value > 0) {
                    stringRedisTemplate.opsForHash().increment(hourlyKey, field, value);
                }
            });
            stringRedisTemplate.expire(hourlyKey, properties.getRetentionDays(), TimeUnit.DAYS);
        });
    }

    private void flushRpsAggregate() {
        aggregateRpsCounter.forEach((rpsKey, counter) -> {
            long value = counter.getAndSet(0);
            if (value > 0) {
                stringRedisTemplate.opsForValue().increment(rpsKey, value);
                stringRedisTemplate.expire(rpsKey, 120, TimeUnit.SECONDS);
            }
        });
    }

    public void checkRedisRecovery() {
        if (redisDegraded && (System.currentTimeMillis() - lastDegradationTime) > properties.getDegradationCheckIntervalMs()) {
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
        flushAggregatedMetrics();
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
            return (value != null ? Long.parseLong(value.toString()) : 0L) + getAggregateStatsValue(dateStr, field);
        } catch (Exception e) {
            log.error("[流量采集] 获取统计字段失败, dateStr={}, field={}, error={}", dateStr, field, e.getMessage());

            String counterKey = STATS_KEY_PREFIX + dateStr + ":" + field;
            AtomicInteger fallbackValue = fallbackStatsCounter.get(counterKey);
            long fallback = fallbackValue != null ? fallbackValue.get() : 0L;
            return fallback + getAggregateStatsValue(dateStr, field);
        }
    }

    private long getAggregateStatsValue(String dateStr, String field) {
        AtomicLong value = aggregateStatsCounter.get(dateStr + ":" + field);
        return value != null ? value.get() : 0L;
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
        for (int i = 0; i < properties.getRetentionDays(); i++) {
            dates.add(today.minusDays(i).format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        return dates;
    }

    private void incrementApiRpsCounter(String apiCode) {
        long currentSecond = System.currentTimeMillis() / 1000;
        String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + currentSecond;
        aggregateRpsCounter.computeIfAbsent(rpsKey, key -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Integer> getApiRpsForSecond(String apiCode, long timestampSecond) {
        Map<String, Integer> result = new HashMap<>();
        try {
            String rpsKey = RPS_KEY_PREFIX + apiCode + ":" + timestampSecond;
            String value = stringRedisTemplate.opsForValue().get(rpsKey);
            int local = getAggregateRpsValue(rpsKey);
            result.put("current", (value != null ? Integer.parseInt(value) : 0) + local);
        } catch (Exception e) {
            log.warn("[流量采集] 获取RPS失败, apiCode={}, error={}", apiCode, e.getMessage());
            result.put("current", getAggregateRpsValue(RPS_KEY_PREFIX + apiCode + ":" + timestampSecond));
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
                    total += Integer.parseInt(value) + getAggregateRpsValue(rpsKey);
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
                    int count = Integer.parseInt(value) + getAggregateRpsValue(rpsKey);
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
                history.add((value != null ? Integer.parseInt(value) : 0) + getAggregateRpsValue(RPS_KEY_PREFIX + apiCode + ":" + (currentSecond - i)));
            }
        } catch (Exception e) {
            log.warn("[流量采集] 获取RPS历史失败, apiCode={}, error={}", apiCode, e.getMessage());
            for (int i = history.size(); i < seconds; i++) {
                history.add(0);
            }
        }
        return history;
    }

    private int getAggregateRpsValue(String rpsKey) {
        AtomicLong value = aggregateRpsCounter.get(rpsKey);
        return value != null ? (int) value.get() : 0;
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
