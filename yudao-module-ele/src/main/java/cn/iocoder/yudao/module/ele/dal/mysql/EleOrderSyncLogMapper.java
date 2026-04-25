package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EleOrderSyncLogMapper extends BaseMapperX<EleOrderSyncLog> {

    default EleOrderSyncLog selectLastSync(String platformStoreId) {
        List<EleOrderSyncLog> list = selectList(new LambdaQueryWrapperX<EleOrderSyncLog>()
                .eq(EleOrderSyncLog::getPlatformStoreId, platformStoreId)
                .eq(EleOrderSyncLog::getStatus, 1)
                .orderByDesc(EleOrderSyncLog::getSyncTime)
                .last("LIMIT 1"));
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Select("SELECT " +
            "COUNT(*) AS totalSyncCount, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS successCount, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS failCount, " +
            "MAX(sync_time) AS lastSyncTime " +
            "FROM ele_order_sync_log " +
            "WHERE platform_store_id = #{platformStoreId}")
    Map<String, Object> selectStoreStats(@Param("platformStoreId") String platformStoreId);

    @Select("SELECT AVG(TIMESTAMPDIFF(SECOND, sync_start_time, sync_end_time)) AS avgDuration " +
            "FROM ele_order_sync_log " +
            "WHERE platform_store_id = #{platformStoreId} " +
            "AND sync_start_time IS NOT NULL " +
            "AND sync_end_time IS NOT NULL")
    Double selectAvgDuration(@Param("platformStoreId") String platformStoreId);

    default EleOrderSyncLog selectByStoreId(String platformStoreId) {
        List<EleOrderSyncLog> list = selectList(new LambdaQueryWrapperX<EleOrderSyncLog>()
                .eq(EleOrderSyncLog::getPlatformStoreId, platformStoreId)
                .orderByDesc(EleOrderSyncLog::getSyncTime)
                .last("LIMIT 1"));
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }
}