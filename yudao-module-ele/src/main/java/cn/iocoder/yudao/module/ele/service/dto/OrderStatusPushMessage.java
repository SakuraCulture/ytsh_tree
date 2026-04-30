package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatusPushMessage implements Serializable {
    private String orderId;
    private String channelOrderId;
    private String platformStoreId;
    private String erpStoreCode;
    private String merchantCode;
    private Integer status;
    private String ticket;
    private String source;
    private String cmd;
    private Long pushTime;
}
