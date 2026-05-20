package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EleStoreInventoryBatchTaskMapper extends BaseMapperX<EleStoreInventoryBatchTaskDO> {

    List<String> RUNNING_STATUS_LIST = List.of("PENDING", "RUNNING");

    @Select("""
            SELECT t.*
            FROM ele_store_inventory_batch_task t
            INNER JOIN ele_store_inventory_batch_task_store s ON s.task_id = t.id
            WHERE t.scope = 'CURRENT_STORE'
              AND s.erp_store_code = #{erpStoreCode}
              AND t.status IN ('PENDING', 'RUNNING')
            ORDER BY t.create_time DESC
            LIMIT 1
            """)
    EleStoreInventoryBatchTaskDO selectRunningCurrentStore(@Param("erpStoreCode") String erpStoreCode);

    default EleStoreInventoryBatchTaskDO selectRunningAllOpenStores() {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .eq(EleStoreInventoryBatchTaskDO::getScope, "ALL_OPEN_STORES")
                .in(EleStoreInventoryBatchTaskDO::getStatus, RUNNING_STATUS_LIST)
                .orderByDesc(EleStoreInventoryBatchTaskDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default List<EleStoreInventoryBatchTaskDO> selectTimeoutPendingTasks(LocalDateTime timeoutAt) {
        return selectList(new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "PENDING")
                .lt(EleStoreInventoryBatchTaskDO::getCreateTime, timeoutAt)
                .orderByAsc(EleStoreInventoryBatchTaskDO::getCreateTime));
    }

    default List<EleStoreInventoryBatchTaskDO> selectTimeoutRunningTasks(LocalDateTime timeoutAt) {
        return selectList(new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "RUNNING")
                .and(wrapper -> wrapper.lt(EleStoreInventoryBatchTaskDO::getStartedAt, timeoutAt)
                        .or()
                        .isNull(EleStoreInventoryBatchTaskDO::getStartedAt)
                        .lt(EleStoreInventoryBatchTaskDO::getCreateTime, timeoutAt))
                .orderByAsc(EleStoreInventoryBatchTaskDO::getStartedAt)
                .orderByAsc(EleStoreInventoryBatchTaskDO::getCreateTime));
    }

    default int markRunningIfPending(Long taskId, LocalDateTime startedAt) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreInventoryBatchTaskDO>()
                .set(EleStoreInventoryBatchTaskDO::getStatus, "RUNNING")
                .set(EleStoreInventoryBatchTaskDO::getStartedAt, startedAt)
                .eq(EleStoreInventoryBatchTaskDO::getId, taskId)
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "PENDING"));
    }

    default int markFailedIfRunning(Long taskId, String errorMsg, LocalDateTime finishedAt) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreInventoryBatchTaskDO>()
                .set(EleStoreInventoryBatchTaskDO::getStatus, "FAILED")
                .set(EleStoreInventoryBatchTaskDO::getErrorMsg, errorMsg)
                .set(EleStoreInventoryBatchTaskDO::getFinishedAt, finishedAt)
                .eq(EleStoreInventoryBatchTaskDO::getId, taskId)
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "RUNNING"));
    }

    default int refreshAggregateIfRunning(Long taskId, EleStoreInventoryBatchTaskDO updateObj) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreInventoryBatchTaskDO>()
                .set(EleStoreInventoryBatchTaskDO::getFinishedStoreCount, updateObj.getFinishedStoreCount())
                .set(EleStoreInventoryBatchTaskDO::getTotalBatchCount, updateObj.getTotalBatchCount())
                .set(EleStoreInventoryBatchTaskDO::getFinishedBatchCount, updateObj.getFinishedBatchCount())
                .set(EleStoreInventoryBatchTaskDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreInventoryBatchTaskDO::getFormalSuccessCount, updateObj.getFormalSuccessCount())
                .set(EleStoreInventoryBatchTaskDO::getShadowSuccessCount, updateObj.getShadowSuccessCount())
                .set(EleStoreInventoryBatchTaskDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreInventoryBatchTaskDO::getFailureCount, updateObj.getFailureCount())
                .set(EleStoreInventoryBatchTaskDO::getErrorMsg, updateObj.getErrorMsg())
                .eq(EleStoreInventoryBatchTaskDO::getId, taskId)
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "RUNNING"));
    }

    default int finishIfRunning(Long taskId, String status, EleStoreInventoryBatchTaskDO updateObj) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreInventoryBatchTaskDO>()
                .set(EleStoreInventoryBatchTaskDO::getStatus, status)
                .set(EleStoreInventoryBatchTaskDO::getFinishedStoreCount, updateObj.getFinishedStoreCount())
                .set(EleStoreInventoryBatchTaskDO::getTotalBatchCount, updateObj.getTotalBatchCount())
                .set(EleStoreInventoryBatchTaskDO::getFinishedBatchCount, updateObj.getFinishedBatchCount())
                .set(EleStoreInventoryBatchTaskDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreInventoryBatchTaskDO::getFormalSuccessCount, updateObj.getFormalSuccessCount())
                .set(EleStoreInventoryBatchTaskDO::getShadowSuccessCount, updateObj.getShadowSuccessCount())
                .set(EleStoreInventoryBatchTaskDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreInventoryBatchTaskDO::getFailureCount, updateObj.getFailureCount())
                .set(EleStoreInventoryBatchTaskDO::getErrorMsg, updateObj.getErrorMsg())
                .set(EleStoreInventoryBatchTaskDO::getFinishedAt, updateObj.getFinishedAt())
                .eq(EleStoreInventoryBatchTaskDO::getId, taskId)
                .eq(EleStoreInventoryBatchTaskDO::getStatus, "RUNNING"));
    }

    default PageResult<EleStoreInventoryBatchTaskDO> selectPage(EleStoreInventoryBatchTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .likeIfPresent(EleStoreInventoryBatchTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getScope, reqVO.getScope())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(EleStoreInventoryBatchTaskDO::getCreateTime));
    }
}
