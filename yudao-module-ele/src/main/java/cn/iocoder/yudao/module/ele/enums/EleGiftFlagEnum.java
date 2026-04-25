package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleGiftFlagEnum {

    YES(1, "是"),
    NO(0, "否");

    private final Integer flag;
    private final String name;

    public Integer getFlag() {
        return flag;
    }

    public String getName() {
        return name;
    }

    public static EleGiftFlagEnum getByFlag(Integer flag) {
        for (EleGiftFlagEnum eleGiftFlagEnum : values()) {
            if (eleGiftFlagEnum.flag.equals(flag)) {
                return eleGiftFlagEnum;
            }
        }
        return NO;
    }

    public static String getNameByFlag(Integer flag) {
        EleGiftFlagEnum flagEnum = getByFlag(flag);
        return flagEnum.name;
    }

    public static boolean isGift(Integer flag) {
        return flag != null && flag == 1;
    }
}
