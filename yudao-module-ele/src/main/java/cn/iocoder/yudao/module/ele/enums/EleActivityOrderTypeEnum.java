package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleActivityOrderTypeEnum {

    MAIN(1, "主单"),
    SUB(0, "子单");

    private final Integer type;
    private final String name;

    public static EleActivityOrderTypeEnum getByType(Integer type) {
        for (EleActivityOrderTypeEnum enumVal : values()) {
            if (enumVal.getType().equals(type)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByType(Integer type) {
        EleActivityOrderTypeEnum typeEnum = getByType(type);
        return typeEnum != null ? typeEnum.getName() : String.valueOf(type);
    }
}
