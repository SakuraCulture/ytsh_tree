package cn.iocoder.yudao.module.ele.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderCreateBodyDTO {
    
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("platform_shop_id")
    private String platformShopId;
}
