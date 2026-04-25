package cn.iocoder.yudao.module.ele.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EleArriveTypeEnum {

    RESERVATION(1, "预约单"),
    IMMEDIATE(2, "即时单");

    private final Integer type;
    private final String name;

    public static EleArriveTypeEnum getByType(Integer type) {
        for (EleArriveTypeEnum enumVal : values()) {
            if (enumVal.getType().equals(type)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByType(Integer type) {
        EleArriveTypeEnum typeEnum = getByType(type);
        return typeEnum != null ? typeEnum.getName() : String.valueOf(type);
    }
}