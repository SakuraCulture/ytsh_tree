package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EleStoreGoodsFullSyncTaskMapper extends BaseMapperX<EleStoreGoodsFullSyncTaskDO> {

    List<String> RUNNING_STATUS_LIST = List.of("PENDING", "RUNNING");

    default EleStoreGoodsFullSyncTaskDO selectRunningCurrentStore(String erpStoreCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskDO>()
                .eq(EleStoreGoodsFullSyncTaskDO::getScope, "CURRENT_STORE")
                .eq(EleStoreGoodsFullSyncTaskDO::getErpStoreCode, erpStoreCode)
                .in(EleStoreGoodsFullSyncTaskDO::getStatus, RUNNING_STATUS_LIST)
                .orderByDesc(EleStoreGoodsFullSyncTaskDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default EleStoreGoodsFullSyncTaskDO selectRunningAllOpenStores() {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskDO>()
                .eq(EleStoreGoodsFullSyncTaskDO::getScope, "ALL_OPEN_STORES")
                .in(EleStoreGoodsFullSyncTaskDO::getStatus, RUNNING_STATUS_LIST)
                .orderByDesc(EleStoreGoodsFullSyncTaskDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default List<EleStoreGoodsFullSyncTaskDO> selectTimeoutPendingTasks(LocalDateTime timeoutAt) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskDO>()
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "PENDING")
                .lt(EleStoreGoodsFullSyncTaskDO::getCreateTime, timeoutAt)
                .orderByAsc(EleStoreGoodsFullSyncTaskDO::getCreateTime));
    }

    default List<EleStoreGoodsFullSyncTaskDO> selectTimeoutRunningTasks(LocalDateTime timeoutAt) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskDO>()
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "RUNNING")
                .and(wrapper -> wrapper.lt(EleStoreGoodsFullSyncTaskDO::getStartedAt, timeoutAt)
                        .or()
                        .isNull(EleStoreGoodsFullSyncTaskDO::getStartedAt)
                        .lt(EleStoreGoodsFullSyncTaskDO::getCreateTime, timeoutAt))
                .orderByAsc(EleStoreGoodsFullSyncTaskDO::getStartedAt)
                .orderByAsc(EleStoreGoodsFullSyncTaskDO::getCreateTime));
    }

    default int markRunningIfPending(Long taskId, LocalDateTime startedAt) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskDO>()
                .set(EleStoreGoodsFullSyncTaskDO::getStatus, "RUNNING")
                .set(EleStoreGoodsFullSyncTaskDO::getStartedAt, startedAt)
                .eq(EleStoreGoodsFullSyncTaskDO::getId, taskId)
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "PENDING"));
    }

    default int markFailedIfRunning(Long taskId, String errorMsg, LocalDateTime finishedAt) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskDO>()
                .set(EleStoreGoodsFullSyncTaskDO::getStatus, "FAILED")
                .set(EleStoreGoodsFullSyncTaskDO::getErrorMsg, errorMsg)
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedAt, finishedAt)
                .eq(EleStoreGoodsFullSyncTaskDO::getId, taskId)
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "RUNNING"));
    }

    default int refreshAggregateIfRunning(Long taskId, EleStoreGoodsFullSyncTaskDO updateObj) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskDO>()
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedStoreCount, updateObj.getFinishedStoreCount())
                .set(EleStoreGoodsFullSyncTaskDO::getTotalPageCount, updateObj.getTotalPageCount())
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedPageCount, updateObj.getFinishedPageCount())
                .set(EleStoreGoodsFullSyncTaskDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreGoodsFullSyncTaskDO::getSuccessCount, updateObj.getSuccessCount())
                .set(EleStoreGoodsFullSyncTaskDO::getFailCount, updateObj.getFailCount())
                .set(EleStoreGoodsFullSyncTaskDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreGoodsFullSyncTaskDO::getErrorMsg, updateObj.getErrorMsg())
                .eq(EleStoreGoodsFullSyncTaskDO::getId, taskId)
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "RUNNING"));
    }

    default int finishIfRunning(Long taskId, String status, EleStoreGoodsFullSyncTaskDO updateObj) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskDO>()
                .set(EleStoreGoodsFullSyncTaskDO::getStatus, status)
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedStoreCount, updateObj.getFinishedStoreCount())
                .set(EleStoreGoodsFullSyncTaskDO::getTotalPageCount, updateObj.getTotalPageCount())
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedPageCount, updateObj.getFinishedPageCount())
                .set(EleStoreGoodsFullSyncTaskDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreGoodsFullSyncTaskDO::getSuccessCount, updateObj.getSuccessCount())
                .set(EleStoreGoodsFullSyncTaskDO::getFailCount, updateObj.getFailCount())
                .set(EleStoreGoodsFullSyncTaskDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreGoodsFullSyncTaskDO::getErrorMsg, updateObj.getErrorMsg())
                .set(EleStoreGoodsFullSyncTaskDO::getFinishedAt, updateObj.getFinishedAt())
                .eq(EleStoreGoodsFullSyncTaskDO::getId, taskId)
                .eq(EleStoreGoodsFullSyncTaskDO::getStatus, "RUNNING"));
    }

    default PageResult<EleStoreGoodsFullSyncTaskDO> selectPage(EleStoreGoodsFullSyncTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskDO>()
                .likeIfPresent(EleStoreGoodsFullSyncTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(EleStoreGoodsFullSyncTaskDO::getScope, reqVO.getScope())
                .eqIfPresent(EleStoreGoodsFullSyncTaskDO::getStatus, reqVO.getStatus())
                .likeIfPresent(EleStoreGoodsFullSyncTaskDO::getMerchantCode, reqVO.getMerchantCode())
                .likeIfPresent(EleStoreGoodsFullSyncTaskDO::getErpStoreCode, reqVO.getErpStoreCode())
                .orderByDesc(EleStoreGoodsFullSyncTaskDO::getCreateTime));
    }
}
