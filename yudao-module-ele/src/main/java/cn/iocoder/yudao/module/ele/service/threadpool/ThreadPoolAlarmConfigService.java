package cn.iocoder.yudao.module.ele.service.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThreadPoolAlarmConfigService {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolAlarmConfigService.class);

    private static final int DEFAULT_QUEUE_THRESHOLD = 80;
    private static final int DEFAULT_ACTIVE_THRESHOLD = 90;

    private final Map<String, PoolAlarmConfig> alarmConfigs = new ConcurrentHashMap<>();

    public PoolAlarmConfig getAlarmConfig(String poolName) {
        return alarmConfigs.computeIfAbsent(poolName, name -> {
            PoolAlarmConfig config = new PoolAlarmConfig();
            config.setPoolName(name);
            config.setQueueThresholdPercent(DEFAULT_QUEUE_THRESHOLD);
            config.setActiveThresholdPercent(DEFAULT_ACTIVE_THRESHOLD);
            config.setEnabled(true);
            return config;
        });
    }

    public void updateAlarmConfig(String poolName, Integer queueThreshold, Integer activeThreshold, Boolean enabled) {
        PoolAlarmConfig config = getAlarmConfig(poolName);
        if (queueThreshold != null) {
            config.setQueueThresholdPercent(queueThreshold);
        }
        if (activeThreshold != null) {
            config.setActiveThresholdPercent(activeThreshold);
        }
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        log.info("[线程池报警] 更新配置: poolName={}, queueThreshold={}%, activeThreshold={}%, enabled={}",
                poolName, config.getQueueThresholdPercent(), config.getActiveThresholdPercent(), config.isEnabled());
    }

    public Map<String, PoolAlarmConfig> getAllConfigs() {
        return new ConcurrentHashMap<>(alarmConfigs);
    }

    public boolean isAlarmEnabled(String poolName) {
        return getAlarmConfig(poolName).isEnabled();
    }

    public static class PoolAlarmConfig {
        private String poolName;
        private int queueThresholdPercent = DEFAULT_QUEUE_THRESHOLD;
        private int activeThresholdPercent = DEFAULT_ACTIVE_THRESHOLD;
        private boolean enabled = true;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public int getQueueThresholdPercent() {
            return queueThresholdPercent;
        }

        public void setQueueThresholdPercent(int queueThresholdPercent) {
            this.queueThresholdPercent = queueThresholdPercent;
        }

        public int getActiveThresholdPercent() {
            return activeThresholdPercent;
        }

        public void setActiveThresholdPercent(int activeThresholdPercent) {
            this.activeThresholdPercent = activeThresholdPercent;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
