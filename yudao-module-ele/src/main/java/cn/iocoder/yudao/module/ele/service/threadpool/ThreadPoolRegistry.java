package cn.iocoder.yudao.module.ele.service.threadpool;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThreadPoolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolRegistry.class);

    @Resource
    private Map<String, ThreadPoolTaskExecutor> allThreadPools;

    public Map<String, ThreadPoolTaskExecutor> getAllPools() {
        if (allThreadPools == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(allThreadPools));
    }

    public ThreadPoolTaskExecutor getPool(String name) {
        return allThreadPools != null ? allThreadPools.get(name) : null;
    }

    public int getPoolCount() {
        return allThreadPools != null ? allThreadPools.size() : 0;
    }
}
