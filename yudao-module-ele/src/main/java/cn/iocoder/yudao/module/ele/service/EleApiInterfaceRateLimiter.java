package cn.iocoder.yudao.module.ele.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EleApiInterfaceRateLimiter implements EleApiRateLimiter {

    private static final String RATE_LIMITER_KEY_PREFIX = "rate_limiter:ele:api:";

    private final RedissonClient redissonClient;
    private final EleApiRateLimitProperties properties;
    private final Map<String, AtomicInteger> waitingCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> backlogLogged = new ConcurrentHashMap<>();

    public EleApiInterfaceRateLimiter(RedissonClient redissonClient, EleApiRateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        properties.getApis().forEach((apiName, limit) -> {
            waitingCounters.computeIfAbsent(apiName, key -> new AtomicInteger(0));
            backlogLogged.computeIfAbsent(apiName, key -> new AtomicInteger(0));
            if (properties.isEnabled()) {
                refreshRateLimiter(apiName, limit);
            }
        });
        log.info("【翱象限流】接口级限流初始化完成，enabled={}，apis={}", properties.isEnabled(), properties.getApis().keySet());
    }

    @Override
    public void acquirePermit(String apiName) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedApiName = normalizeApiName(apiName);
        EleApiRateLimitProperties.ApiLimit limit = getLimit(normalizedApiName);
        RRateLimiter rateLimiter = getRateLimiter(normalizedApiName, limit);
        if (rateLimiter.tryAcquire()) {
            return;
        }

        AtomicInteger waitingCounter = waitingCounters.computeIfAbsent(normalizedApiName, key -> new AtomicInteger(0));
        AtomicInteger loggedFlag = backlogLogged.computeIfAbsent(normalizedApiName, key -> new AtomicInteger(0));
        int waiting = waitingCounter.incrementAndGet();
        long startTime = System.currentTimeMillis();
        if (loggedFlag.compareAndSet(0, 1)) {
            log.warn("【翱象限流】api={}({}) 出现排队，当前本地等待数={}，接口QPS上限={}",
                    normalizedApiName, limit.getApiCode(), waiting, limit.getQps());
        }

        try {
            while (!rateLimiter.tryAcquire()) {
                sleepQuietly(normalizedApiName);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("RateLimiter is not initialized")) {
                log.warn("【翱象限流】api={} 限流器未初始化，尝试重新初始化", normalizedApiName);
                refreshRateLimiter(normalizedApiName, limit);
                acquirePermit(normalizedApiName);
                return;
            }
            throw e;
        } finally {
            int remain = waitingCounter.decrementAndGet();
            long waitMs = System.currentTimeMillis() - startTime;
            if (remain == 0) {
                loggedFlag.set(0);
                log.info("【翱象限流】api={} 排队清空，最后等待耗时={}ms", normalizedApiName, waitMs);
            } else if (waitMs > 1000) {
                log.info("【翱象限流】api={} 获取令牌成功，等待耗时={}ms，剩余等待数={}",
                        normalizedApiName, waitMs, remain);
            }
        }
    }

    @Override
    public boolean hasBacklog() {
        if (!properties.isBacklogPauseEnabled()) {
            return false;
        }
        return waitingCounters.values().stream().anyMatch(counter -> counter.get() > 0);
    }

    @Override
    public int getLocalWaitingCount() {
        return waitingCounters.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    @Override
    public long getGlobalQps() {
        return properties.getApis().values().stream().mapToLong(EleApiRateLimitProperties.ApiLimit::getQps).sum();
    }

    @Override
    public List<ApiRateLimitStatus> getApiStatuses() {
        List<ApiRateLimitStatus> statuses = new ArrayList<>();
        properties.getApis().forEach((apiName, limit) -> {
            int waiting = waitingCounters.computeIfAbsent(apiName, key -> new AtomicInteger(0)).get();
            statuses.add(new ApiRateLimitStatus(apiName, limit.getApiCode(), limit.getDisplayName(), limit.getQps(), waiting));
        });
        return statuses;
    }

    private RRateLimiter getRateLimiter(String apiName, EleApiRateLimitProperties.ApiLimit limit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(buildRateLimiterKey(apiName));
        if (!rateLimiter.isExists()) {
            refreshRateLimiter(apiName, limit);
        }
        return rateLimiter;
    }

    private synchronized void refreshRateLimiter(String apiName, EleApiRateLimitProperties.ApiLimit limit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(buildRateLimiterKey(apiName));
        rateLimiter.setRate(RateType.OVERALL, limit.getQps(), Duration.ofSeconds(1));
        log.info("【翱象限流】刷新接口限流器配置，api={}，apiCode={}，qps={}", apiName, limit.getApiCode(), limit.getQps());
    }

    private EleApiRateLimitProperties.ApiLimit getLimit(String apiName) {
        EleApiRateLimitProperties.ApiLimit limit = properties.getApis().get(apiName);
        if (limit == null) {
            throw new IllegalArgumentException("未配置翱象接口限流参数，apiName=" + apiName);
        }
        if (limit.getQps() <= 0) {
            throw new IllegalArgumentException("翱象接口限流QPS必须大于0，apiName=" + apiName);
        }
        return limit;
    }

    private String normalizeApiName(String apiName) {
        if (API_ORDER_LIST.equals(apiName) || "orderList".equals(apiName)) {
            return API_ORDER_LIST;
        }
        if (API_ORDER_GET.equals(apiName) || "orderDetail".equals(apiName) || "orderGet".equals(apiName)) {
            return API_ORDER_GET;
        }
        if (API_SKU_STOCK_INVENTORY_BATCH_QUERY.equals(apiName)
                || "SKU_STOCK_INVENTORY_BATCH_QUERY".equals(apiName)) {
            return API_SKU_STOCK_INVENTORY_BATCH_QUERY;
        }
        return apiName;
    }

    private String buildRateLimiterKey(String apiName) {
        return RATE_LIMITER_KEY_PREFIX + apiName;
    }

    private void sleepQuietly(String apiName) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(1, properties.getWaitIntervalMs()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待翱象接口限流许可时被中断，api=" + apiName, e);
        }
    }
}
