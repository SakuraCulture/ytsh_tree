package cn.iocoder.yudao.module.ele.service;

import java.util.List;

/**
 * 翱象 API 接口级全局限流器。
 *
 * <p>所有应用实例共享同一组 Redis 限流器，并按翱象接口维度分别控制 QPS。</p>
 */
public interface EleApiRateLimiter {

    String API_ORDER_LIST = "orderList";
    String API_ORDER_GET = "orderGet";
    String API_SKU_STOCK_INVENTORY_BATCH_QUERY = "skuStockInventoryBatchQuery";

    /**
     * 获取指定接口的全局调用许可；超过接口 QPS 时阻塞排队等待。
     *
     * @param apiName 接口名称，可传 orderList / orderGet
     */
    void acquirePermit(String apiName);

    /**
     * 当前实例任意接口是否存在等待获取令牌的请求。
     */
    boolean hasBacklog();

    /**
     * 当前实例全部接口正在等待令牌的请求数量。
     */
    int getLocalWaitingCount();

    /**
     * 兼容旧状态接口：返回全部接口配置 QPS 之和。
     */
    long getGlobalQps();

    /**
     * 获取每个接口的限流状态。
     */
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
