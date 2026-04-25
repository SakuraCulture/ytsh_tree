package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleChannelTypeEnum {

    ELE("ELE", "淘宝闪购"),
    MT("MT", "美团"),
    JD("JD", "京东");

    private final String code;
    private final String name;

    public static EleChannelTypeEnum getByCode(String code) {
        for (EleChannelTypeEnum enumVal : values()) {
            if (enumVal.getCode().equals(code)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        EleChannelTypeEnum channelEnum = getByCode(code);
        return channelEnum != null ? channelEnum.getName() : code;
    }
}