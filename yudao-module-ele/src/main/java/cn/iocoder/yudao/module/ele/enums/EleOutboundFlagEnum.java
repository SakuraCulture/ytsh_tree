package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleOutboundFlagEnum {

    NOT_OUTBOUND(0, "未出库"),
    OUTBOUND(1, "已出库");

    private final Integer flag;
    private final String name;

    public static EleOutboundFlagEnum getByFlag(Integer flag) {
        if (flag == null) {
            return null;
        }
        for (EleOutboundFlagEnum eleOutboundFlagEnum : values()) {
            if (eleOutboundFlagEnum.getFlag().equals(flag)) {
                return eleOutboundFlagEnum;
            }
        }
        return null;
    }

    public static String getNameByFlag(Integer flag) {
        EleOutboundFlagEnum flagEnum = getByFlag(flag);
        return flagEnum != null ? flagEnum.getName() : String.valueOf(flag);
    }

    public static boolean isOutbound(Integer flag) {
        return OUTBOUND.getFlag().equals(flag);
    }
}