package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.BillSyncFailLogDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BillSyncFailLogMapper extends BaseMapperX<BillSyncFailLogDO> {

    @Insert("INSERT INTO bill_sync_fail_log " +
            "(bill_date, merchant_code, store_code, store_name, market_user_id, " +
            "fail_page, fail_reason, retry_count, retry_status, sync_time, create_time, update_time, tenant_id, deleted) " +
            "VALUES " +
            "(#{billDate}, #{merchantCode}, #{storeCode}, #{storeName}, #{marketUserId}, " +
            "#{failPage}, #{failReason}, #{retryCount}, #{retryStatus}, #{syncTime}, #{createTime}, #{updateTime}, #{tenantId}, #{deleted}) " +
            "ON DUPLICATE KEY UPDATE " +
            "fail_reason=VALUES(fail_reason), fail_page=VALUES(fail_page), " +
            "retry_count=retry_count + 1, sync_time=VALUES(sync_time), update_time=VALUES(update_time)")
    void rawInsertOrUpdate(BillSyncFailLogDO logDO);

    @Select("SELECT * FROM bill_sync_fail_log WHERE deleted = 0 " +
            "AND retry_status = 0 ORDER BY sync_time DESC LIMIT #{limit}")
    List<BillSyncFailLogDO> selectList(@Param("limit") int limit);

    @Select("SELECT * FROM bill_sync_fail_log WHERE deleted = 0 " +
            "AND retry_status = 0 ORDER BY sync_time ASC LIMIT #{limit}")
    List<BillSyncFailLogDO> selectPendingList(@Param("limit") int limit);

    @Select("SELECT * FROM bill_sync_fail_log WHERE deleted = 0 AND bill_date = #{billDate} " +
            "AND merchant_code = #{merchantCode} AND store_code = #{storeCode} LIMIT 1")
    BillSyncFailLogDO selectByStoreAndDate(@Param("billDate") LocalDate billDate,
                                           @Param("merchantCode") String merchantCode,
                                           @Param("storeCode") String storeCode);

    @Select("SELECT COUNT(*) FROM bill_sync_fail_log WHERE deleted = 0 AND retry_status = 0")
    int countPending();

    @Update("UPDATE bill_sync_fail_log SET retry_status = 1, last_retry_time = NOW(), " +
            "update_time = #{updateTime} WHERE id = #{id}")
    int markRetrying(@Param("id") Long id, @Param("updateTime") Long updateTime);

    @Update("UPDATE bill_sync_fail_log SET retry_status = 2, resolve_time = NOW(), " +
            "update_time = #{updateTime} WHERE id = #{id}")
    int markResolved(@Param("id") Long id, @Param("updateTime") Long updateTime);

    @Update("UPDATE bill_sync_fail_log SET retry_status = 0, update_time = #{updateTime} WHERE id = #{id}")
    int markPending(@Param("id") Long id, @Param("updateTime") Long updateTime);

    @Update("UPDATE bill_sync_fail_log SET retry_count = #{retryCount}, " +
            "retry_status = 0, fail_reason = #{failReason}, update_time = #{updateTime} WHERE id = #{id}")
    int updateRetryCount(@Param("id") Long id, @Param("retryCount") int retryCount,
                         @Param("failReason") String failReason);
}
