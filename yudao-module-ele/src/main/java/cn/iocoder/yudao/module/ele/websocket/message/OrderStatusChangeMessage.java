package cn.iocoder.yudao.module.ele.websocket.message;

import lombok.Data;

@Data
public class OrderStatusChangeMessage {

    private String orderId;

    private Integer oldStatus;

    private Integer newStatus;

    private String storeName;

    private String buyerName;

    private Boolean soundEnabled;

    private Boolean desktopEnabled;

    private Long timestamp;
}
