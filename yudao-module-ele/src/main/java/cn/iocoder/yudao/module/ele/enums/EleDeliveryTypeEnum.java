package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleDeliveryTypeEnum {

    PLATFORM_DELIVERY(1, "平台配"),
    SELF_DELIVERY(2, "自配送"),
    SELF_PICKUP(3, "自提");

    private final Integer type;
    private final String name;

    public static EleDeliveryTypeEnum getByType(Integer type) {
        for (EleDeliveryTypeEnum enumVal : values()) {
            if (enumVal.getType().equals(type)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByType(Integer type) {
        EleDeliveryTypeEnum typeEnum = getByType(type);
        return typeEnum != null ? typeEnum.getName() : String.valueOf(type);
    }
}