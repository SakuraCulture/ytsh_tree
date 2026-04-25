package cn.iocoder.yudao.module.business.dal.redis;

/**
 * Business 模块 Redis Key 枚举类
 */
public interface BusinessRedisKeyConstants {

    /**
     * 已开店门店的平台信息缓存
     * KEY 格式：store_platform_info
     * VALUE 数据类型：String JSON数组，包含platform_store_id和store_name
     * 过期时间：永不过期
     */
    String STORE_PLATFORM_INFO = "store_platform_info";

}