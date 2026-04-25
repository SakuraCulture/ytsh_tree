package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleDeliveryStatusEnum {

    WAITING_DELIVERY_REQUEST(29, "待请求配送"),
    WAITING_RIDER_ACCEPT(30, "待骑手接单"),
    DELIVERY_CANCELLED(31, "配送取消"),
    RIDER_ACCEPTED(32, "骑手接单"),
    RIDER_ARRIVED(33, "骑手到店"),
    DELIVERY_EXCEPTION(34, "配送异常"),
    RIDER_PICKED_UP(35, "骑手揽收"),
    RIDER_DELIVERED(36, "骑手送达");

    private final Integer status;
    private final String name;

    public static EleDeliveryStatusEnum getByStatus(Integer status) {
        for (EleDeliveryStatusEnum eleDeliveryStatusEnum : values()) {
            if (eleDeliveryStatusEnum.getStatus().equals(status)) {
                return eleDeliveryStatusEnum;
            }
        }
        return null;
    }

    public static String getNameByStatus(Integer status) {
        EleDeliveryStatusEnum statusEnum = getByStatus(status);
        return statusEnum != null ? statusEnum.getName() : String.valueOf(status);
    }
}
