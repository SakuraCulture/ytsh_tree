package cn.iocoder.yudao.module.ele.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ele.api.rate-limit")
public class EleApiRateLimitProperties {

    private boolean enabled = true;

    private long waitIntervalMs = 10;

    private boolean backlogPauseEnabled = true;

    private Map<String, ApiLimit> apis = defaultApis();

    private static Map<String, ApiLimit> defaultApis() {
        Map<String, ApiLimit> defaults = new LinkedHashMap<>();
        defaults.put(EleApiRateLimiter.API_ORDER_LIST,
                new ApiLimit("me.ele.retail:saas.order.list", "订单列表查询", 180));
        defaults.put(EleApiRateLimiter.API_ORDER_GET,
                new ApiLimit("me.ele.retail:saas.order.get", "正向订单列表查询", 360));
        defaults.put(EleApiRateLimiter.API_SKU_STOCK_INVENTORY_BATCH_QUERY,
                new ApiLimit("me.ele.retail:saas.sku.stock.inventoy.batch.query", "库存批量查询", 50));
        return defaults;
    }

    @Data
    public static class ApiLimit {
        private String apiCode;
        private String displayName;
        private long qps;

        public ApiLimit() {
        }

        public ApiLimit(String apiCode, String displayName, long qps) {
            this.apiCode = apiCode;
            this.displayName = displayName;
            this.qps = qps;
        }
    }
}
