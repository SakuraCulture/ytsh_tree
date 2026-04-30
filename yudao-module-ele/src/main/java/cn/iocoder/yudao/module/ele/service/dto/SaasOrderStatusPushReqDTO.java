package cn.iocoder.yudao.module.ele.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class SaasOrderStatusPushReqDTO implements Serializable {
    
    @JsonProperty("cmd")
    private String cmd;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("ticket")
    private String ticket;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("sign")
    private String sign;
    
    @JsonProperty("encrypt")
    private String encrypt;
    
    @JsonProperty("market_user_id")
    private String marketUserId;
    
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("channel_order_id")
    private String channelOrderId;
    
    @JsonProperty("erp_store_code")
    private String erpStoreCode;
    
    @JsonProperty("merchant_code")
    private String merchantCode;
    
    @JsonProperty("status")
    private Integer status;
}
