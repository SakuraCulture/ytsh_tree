package cn.iocoder.yudao.module.ele.dal.redis;

public class EleLockKeyConstants {

    public static final String ORDER_SYNC_LOCK = "ele:order:sync:lock:%s";

    public static final String ORDER_COMPENSATE_LOCK = "ele:order:compensate:lock:%s";

    public static final String ORDER_ITEM_LOCK = "ele:order:item:lock:%s";

    private EleLockKeyConstants() {
    }
}