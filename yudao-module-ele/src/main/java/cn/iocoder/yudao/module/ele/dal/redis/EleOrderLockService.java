package cn.iocoder.yudao.module.ele.dal.redis;

public interface EleOrderLockService {

    void lockSync(String platformStoreId, int waitSeconds, int leaseMinutes);

    void lockCompensate(String taskId, int waitSeconds, int leaseMinutes);

    boolean tryLockSync(String platformStoreId, int waitSeconds, int leaseMinutes);

    boolean tryLockCompensate(String taskId, int waitSeconds, int leaseMinutes);

    boolean tryLockSyncWindow(String platformStoreId, long windowStart, long windowEnd, int waitSeconds, int leaseMinutes);

    void unlockSyncWindow(String platformStoreId, long windowStart, long windowEnd);

    void unlockSync(String platformStoreId);

    void unlockCompensate(String taskId);

    boolean tryLockOrder(String orderId, int waitSeconds, int leaseSeconds);

    boolean tryLockOrderWithWatchdog(String orderId, int waitSeconds);

    void unlockOrder(String orderId);

    void lockStoreGoodsFullSyncTask(String lockKey, int waitSeconds, int leaseMinutes);

    void unlockStoreGoodsFullSyncTask(String lockKey);

    void lockStoreInventoryBatchTask(String lockKey, int waitSeconds, int leaseMinutes);

    void unlockStoreInventoryBatchTask(String lockKey);
}
