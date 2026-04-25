package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EleDeliveryPlatformEnum {

    MAI_YA_TIAN(1, "麦芽田"),
    FENG_NIAO(2, "蜂鸟"),
    HUA_JI_TONG(3, "花集通"),
    JU_HAO_SONG(4, "聚好送"),
    KUAI_DAO_JIA(5, "快到家");

    private final Integer platform;
    private final String name;

    public static EleDeliveryPlatformEnum getByPlatform(Integer platform) {
        for (EleDeliveryPlatformEnum enumVal : values()) {
            if (enumVal.getPlatform().equals(platform)) {
                return enumVal;
            }
        }
        return null;
    }

    public static String getNameByPlatform(Integer platform) {
        EleDeliveryPlatformEnum platformEnum = getByPlatform(platform);
        return platformEnum != null ? platformEnum.getName() : String.valueOf(platform);
    }
}