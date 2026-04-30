package cn.iocoder.yudao.module.ele.websocket.message;

import lombok.Data;

@Data
public class OrderRemindMessage {

    private String orderId;

    private String channelOrderId;

    private String storeName;

    private String buyerName;

    private Integer remindCount;

    private Long timestamp;
}
