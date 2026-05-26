package cn.iocoder.yudao.module.ele.dal.redis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.ele.service.dto.StoreSyncProgress;
import cn.iocoder.yudao.module.ele.service.dto.SyncErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderSyncProgressCache {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PROGRESS_KEY_PREFIX = "order:sync:progress:";
    private static final String BATCH_KEY_PREFIX = "order:sync:batch:";
    private static final long TEMP_PROTECT_SECONDS = 600;     private static final long COMPLETED_EXPIRE_SECONDS = 60; 
    
    public void updateStoreProgress(String batchId, String platformStoreId,
                                     StoreSyncProgress progress) {
        try {
            String key = PROGRESS_KEY_PREFIX + batchId + ":" + platformStoreId;
            redisTemplate.opsForValue().set(key, serializeProgress(progress));
            redisTemplate.expire(key, TEMP_PROTECT_SECONDS, TimeUnit.SECONDS);

            String storesKey = BATCH_KEY_PREFIX + batchId + ":stores";
            redisTemplate.opsForSet().add(storesKey, platformStoreId);
            redisTemplate.expire(storesKey, TEMP_PROTECT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("【Redis进度缓存】更新进度失败 batchId={} storeId={}", batchId, platformStoreId, e);
        }
    }

    
    public void markStoreCompleted(String batchId, String platformStoreId) {
        try {
            String key = PROGRESS_KEY_PREFIX + batchId + ":" + platformStoreId;
            redisTemplate.expire(key, COMPLETED_EXPIRE_SECONDS, TimeUnit.SECONDS);

            String storesKey = BATCH_KEY_PREFIX + batchId + ":stores";
            redisTemplate.opsForSet().remove(storesKey, platformStoreId);

            Long remainingStores = redisTemplate.opsForSet().size(storesKey);
            if (remainingStores == null || remainingStores == 0) {
                cleanBatchProgress(batchId);
            }
        } catch (Exception e) {
            log.error("【Redis进度缓存】标记门店完成失败 batchId={} storeId={}", batchId, platformStoreId, e);
        }
    }

    
    public void removeStoreProgress(String batchId, String platformStoreId) {
        try {
            String key = PROGRESS_KEY_PREFIX + batchId + ":" + platformStoreId;
            redisTemplate.delete(key);

            String storesKey = BATCH_KEY_PREFIX + batchId + ":stores";
            redisTemplate.opsForSet().remove(storesKey, platformStoreId);

            Long remainingStores = redisTemplate.opsForSet().size(storesKey);
            if (remainingStores == null || remainingStores == 0) {
                cleanBatchProgress(batchId);
            }
        } catch (Exception e) {
            log.error("【Redis进度缓存】删除门店进度失败 batchId={} storeId={}", batchId, platformStoreId, e);
        }
    }

    
    public StoreSyncProgress getStoreProgress(String batchId, String platformStoreId) {
        try {
            String key = PROGRESS_KEY_PREFIX + batchId + ":" + platformStoreId;
            Object data = redisTemplate.opsForValue().get(key);
            if (data == null) {
                return null;
            }
            return deserializeProgress(data);
        } catch (Exception e) {
            log.error("【Redis进度缓存】获取进度失败 batchId={} storeId={}", batchId, platformStoreId, e);
            return null;
        }
    }

    
    public List<StoreSyncProgress> getBatchProgress(String batchId) {
        try {
            String storesKey = BATCH_KEY_PREFIX + batchId + ":stores";
            Set<Object> storeIds = redisTemplate.opsForSet().members(storesKey);

            List<StoreSyncProgress> progressList = new ArrayList<>();
            if (CollUtil.isNotEmpty(storeIds)) {
                for (Object storeIdObj : storeIds) {
                    String storeId = String.valueOf(storeIdObj);
                    StoreSyncProgress progress = getStoreProgress(batchId, storeId);
                    if (progress != null) {
                        progressList.add(progress);
                    }
                }
            }
            return progressList;
        } catch (Exception e) {
            log.error("【Redis进度缓存】获取批次进度失败 batchId={}", batchId, e);
            return Collections.emptyList();
        }
    }

    
    public void cleanBatchProgress(String batchId) {
        try {
            String storesKey = BATCH_KEY_PREFIX + batchId + ":stores";
            Set<Object> storeIds = redisTemplate.opsForSet().members(storesKey);

            if (storeIds != null) {
                List<String> keysToDelete = new ArrayList<>();
                for (Object storeIdObj : storeIds) {
                    keysToDelete.add(PROGRESS_KEY_PREFIX + batchId + ":" + storeIdObj);
                }
                if (!keysToDelete.isEmpty()) {
                    redisTemplate.delete(keysToDelete);
                }
            }
            redisTemplate.delete(storesKey);
            redisTemplate.delete(BATCH_KEY_PREFIX + batchId);
        } catch (Exception e) {
            log.error("【Redis进度缓存】清理批次进度失败 batchId={}", batchId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private StoreSyncProgress deserializeProgress(Object data) {
        try {
            if (data instanceof String) {
                return JSONUtil.toBean((String) data, StoreSyncProgress.class);
            } else if (data instanceof Map) {
                String json = JSONUtil.toJsonStr(data);
                return JSONUtil.toBean(json, StoreSyncProgress.class);
            }
            return null;
        } catch (Exception e) {
            log.error("【Redis进度缓存】反序列化失败", e);
            return null;
        }
    }

    private String serializeProgress(StoreSyncProgress progress) {
        return JSONUtil.toJsonStr(progress);
    }
}
