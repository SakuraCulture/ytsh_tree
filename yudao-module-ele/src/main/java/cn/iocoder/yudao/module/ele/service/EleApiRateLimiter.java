package cn.iocoder.yudao.module.ele.service;

import java.util.List;


public interface EleApiRateLimiter {

    String API_ORDER_LIST = "orderList";
    String API_ORDER_GET = "orderGet";
    String API_SKU_STOCK_INVENTORY_BATCH_QUERY = "skuStockInventoryBatchQuery";

    
    void acquirePermit(String apiName);

    
    boolean hasBacklog();

    
    int getLocalWaitingCount();

    
    long getGlobalQps();

    
    List<ApiRateLimitStatus> getApiStatuses();

    class ApiRateLimitStatus {
        private String apiName;
        private String apiCode;
        private String displayName;
        private long qps;
        private int waitingCount;
        private boolean hasBacklog;

        public ApiRateLimitStatus() {
        }

        public ApiRateLimitStatus(String apiName, String apiCode, String displayName, long qps, int waitingCount) {
            this.apiName = apiName;
            this.apiCode = apiCode;
            this.displayName = displayName;
            this.qps = qps;
            this.waitingCount = waitingCount;
            this.hasBacklog = waitingCount > 0;
        }

        public String getApiName() {
            return apiName;
        }

        public void setApiName(String apiName) {
            this.apiName = apiName;
        }

        public String getApiCode() {
            return apiCode;
        }

        public void setApiCode(String apiCode) {
            this.apiCode = apiCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public long getQps() {
            return qps;
        }

        public void setQps(long qps) {
            this.qps = qps;
        }

        public int getWaitingCount() {
            return waitingCount;
        }

        public void setWaitingCount(int waitingCount) {
            this.waitingCount = waitingCount;
        }

        public boolean isHasBacklog() {
            return hasBacklog;
        }

        public void setHasBacklog(boolean hasBacklog) {
            this.hasBacklog = hasBacklog;
        }
    }
}
