package cn.iocoder.yudao.module.ele.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class EleOrderRetryQueueService {

    private final Map<String, List<RetryTask>> retryQueueByStore = new ConcurrentHashMap<>();
    private final AtomicInteger totalQueueSize = new AtomicInteger(0);

    @Data
    public static class RetryTask {
        private String orderId;
        private String channelOrderId;
        private String platformStoreId;
        private String merchantCode;
        private String erpStoreCode;
        private Long failRecordId;
        private Object orderDetail;

        public RetryTask(String orderId, String channelOrderId, String platformStoreId,
                String merchantCode, String erpStoreCode, Long failRecordId, Object orderDetail) {
            this.orderId = orderId;
            this.channelOrderId = channelOrderId;
            this.platformStoreId = platformStoreId;
            this.merchantCode = merchantCode;
            this.erpStoreCode = erpStoreCode;
            this.failRecordId = failRecordId;
            this.orderDetail = orderDetail;
        }
    }

    public void enqueue(String platformStoreId, RetryTask task) {
        retryQueueByStore.computeIfAbsent(platformStoreId, k -> new ArrayList<>()).add(task);
        totalQueueSize.incrementAndGet();
        log.debug("【重试队列】订单入队，orderId={}, platformStoreId={}, 当前队列大小={}",
                task.getOrderId(), platformStoreId, totalQueueSize.get());
    }

    public List<RetryTask> dequeueAll() {
        List<RetryTask> allTasks = new ArrayList<>();
        for (Map.Entry<String, List<RetryTask>> entry : retryQueueByStore.entrySet()) {
            List<RetryTask> tasks = retryQueueByStore.remove(entry.getKey());
            if (tasks != null) {
                allTasks.addAll(tasks);
                totalQueueSize.addAndGet(-tasks.size());
            }
        }
        log.info("【重试队列】出队全部任务，共{}个订单", allTasks.size());
        return allTasks;
    }

    public List<RetryTask> dequeueByStore(String platformStoreId) {
        List<RetryTask> tasks = retryQueueByStore.remove(platformStoreId);
        if (tasks != null) {
            totalQueueSize.addAndGet(-tasks.size());
            log.info("【重试队列】出队门店{}的任务，共{}个订单", platformStoreId, tasks.size());
        }
        return tasks != null ? tasks : new ArrayList<>();
    }

    public int getQueueSize() {
        return totalQueueSize.get();
    }

    public int getQueueSizeByStore(String platformStoreId) {
        List<RetryTask> tasks = retryQueueByStore.get(platformStoreId);
        return tasks != null ? tasks.size() : 0;
    }

    public Map<String, Integer> getQueueSizeByAllStores() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, List<RetryTask>> entry : retryQueueByStore.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    public void clear() {
        int size = totalQueueSize.get();
        retryQueueByStore.clear();
        totalQueueSize.set(0);
        log.info("【重试队列】清空队列，共清理{}个订单", size);
    }

    public boolean isEmpty() {
        return totalQueueSize.get() == 0;
    }
}
