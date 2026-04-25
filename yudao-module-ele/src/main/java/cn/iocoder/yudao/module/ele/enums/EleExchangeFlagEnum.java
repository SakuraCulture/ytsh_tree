package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleExchangeFlagEnum {

    YES(1, "是"),
    NO(0, "否");

    private final Integer flag;
    private final String name;

    public static EleExchangeFlagEnum getByFlag(Integer flag) {
        for (EleExchangeFlagEnum enumVal : values()) {
            if (enumVal.getFlag().equals(flag)) {
                return enumVal;
            }
        }
        return flag == 1 ? YES : NO;
    }

    public static String getNameByFlag(Integer flag) {
        EleExchangeFlagEnum flagEnum = getByFlag(flag);
        return flagEnum.getName();
    }

    public static boolean isExchange(Integer flag) {
        return flag != null && flag == 1;
    }
}