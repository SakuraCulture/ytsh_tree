package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleGoodsTypeEnum {

    SINGLE(0, "单品"),
    COMBO(3, "组合品");

    private final Integer type;
    private final String name;

    public static EleGoodsTypeEnum getByType(Integer type) {
        for (EleGoodsTypeEnum enumVal : values()) {
            if (enumVal.getType().equals(type)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByType(Integer type) {
        EleGoodsTypeEnum typeEnum = getByType(type);
        return typeEnum != null ? typeEnum.getName() : String.valueOf(type);
    }
}
