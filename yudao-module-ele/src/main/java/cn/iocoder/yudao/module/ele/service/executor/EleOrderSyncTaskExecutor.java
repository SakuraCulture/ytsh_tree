package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;

import java.util.List;


public interface EleOrderSyncTaskExecutor {

    
    SyncResult executeSync(List<StorePlatformRespVO> stores, Long forcedStartTime, Long forcedEndTime);

    
    void setSyncDelegate(StoreSyncDelegate delegate);

    
    @FunctionalInterface
    interface StoreSyncDelegate {
        void syncStore(StorePlatformRespVO store, Long forcedStartTime, Long forcedEndTime);
    }

    
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
