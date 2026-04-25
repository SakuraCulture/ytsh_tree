package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;

import java.util.List;

/**
 * 饿了么订单同步执行器服务
 *
 * @author 优团科技数字化团队
 */
public interface EleOrderSyncTaskExecutor {

    /**
     * 执行多门店并行订单同步
     *
     * @param stores 门店列表
     * @param forcedStartTime 强制起始时间（秒级时间戳，可为 null）
     * @param forcedEndTime 强制结束时间（秒级时间戳，可为 null）
     * @return 同步结果摘要
     */
    SyncResult executeSync(List<StorePlatformRespVO> stores, Long forcedStartTime, Long forcedEndTime);

    /**
     * 注册单店同步委托
     */
    void setSyncDelegate(StoreSyncDelegate delegate);

    /**
     * 单店同步委托接口，由业务实现方注册
     */
    @FunctionalInterface
    interface StoreSyncDelegate {
        void syncStore(StorePlatformRespVO store, Long forcedStartTime, Long forcedEndTime);
    }

    /**
     * 同步结果
     */
    class SyncResult {
        private final int totalCount;
        private final int successCount;
        private final int failCount;
        private final long elapsedSeconds;
        private final List<String> failedStores;
        private final boolean completed;

        public SyncResult(int totalCount, int successCount, int failCount,
                          long elapsedSeconds, List<String> failedStores, boolean completed) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failCount = failCount;
            this.elapsedSeconds = elapsedSeconds;
            this.failedStores = failedStores;
            this.completed = completed;
        }

        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public long getElapsedSeconds() { return elapsedSeconds; }
        public List<String> getFailedStores() { return failedStores; }
        public boolean isCompleted() { return completed; }
    }

}
