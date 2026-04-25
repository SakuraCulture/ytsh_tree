package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleOrderStatusEnum {

    PAID(1, "已支付"),
    ACCEPTED(2, "已接单"),
    PICKED(3, "已拣货"),
    PACKED(4, "已打包"),
    SHIPPED(5, "已发货"),
    COMPLETED(6, "交易成功"),
    CLOSED(-1, "交易关闭");

    private final Integer status;
    private final String name;

    public static EleOrderStatusEnum getByStatus(Integer status) {
        for (EleOrderStatusEnum eleOrderStatusEnum : values()) {
            if (eleOrderStatusEnum.getStatus().equals(status)) {
                return eleOrderStatusEnum;
            }
        }
        return null;
    }

    public static boolean isTerminalStatus(Integer status) {
        return COMPLETED.getStatus().equals(status) || CLOSED.getStatus().equals(status);
    }
}