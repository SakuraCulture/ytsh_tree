package cn.iocoder.yudao.module.ele.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.ele.store-goods-full-sync")
public class EleStoreGoodsFullSyncJobProperties {

    private boolean enabled = false;

    private String cron = "0 0 2 ? * MON";

    private boolean ckSyncEnabled = false;
}
