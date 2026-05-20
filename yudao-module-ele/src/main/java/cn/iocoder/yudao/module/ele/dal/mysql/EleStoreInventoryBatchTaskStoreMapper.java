package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskStoreDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EleStoreInventoryBatchTaskStoreMapper extends BaseMapperX<EleStoreInventoryBatchTaskStoreDO> {

    default List<EleStoreInventoryBatchTaskStoreDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<EleStoreInventoryBatchTaskStoreDO>()
                .eq(EleStoreInventoryBatchTaskStoreDO::getTaskId, taskId)
                .orderByAsc(EleStoreInventoryBatchTaskStoreDO::getId));
    }

    default PageResult<EleStoreInventoryBatchTaskStoreDO> selectPage(EleStoreInventoryBatchTaskStorePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreInventoryBatchTaskStoreDO>()
                .eq(EleStoreInventoryBatchTaskStoreDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(EleStoreInventoryBatchTaskStoreDO::getStatus, reqVO.getStatus())
                .likeIfPresent(EleStoreInventoryBatchTaskStoreDO::getErpStoreCode, reqVO.getErpStoreCode())
                .likeIfPresent(EleStoreInventoryBatchTaskStoreDO::getStoreId, reqVO.getStoreId())
                .orderByAsc(EleStoreInventoryBatchTaskStoreDO::getId));
    }

    default int cancelPendingByTaskId(Long taskId, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<EleStoreInventoryBatchTaskStoreDO>()
                .set(EleStoreInventoryBatchTaskStoreDO::getStatus, "CANCELLED")
                .set(EleStoreInventoryBatchTaskStoreDO::getFinishedAt, finishedAt)
                .eq(EleStoreInventoryBatchTaskStoreDO::getTaskId, taskId)
                .eq(EleStoreInventoryBatchTaskStoreDO::getStatus, "PENDING"));
    }

    default int failUnfinishedByTaskId(Long taskId, String errorMsg, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<EleStoreInventoryBatchTaskStoreDO>()
                .set(EleStoreInventoryBatchTaskStoreDO::getStatus, "FAILED")
                .set(EleStoreInventoryBatchTaskStoreDO::getErrorMsg, errorMsg)
                .set(EleStoreInventoryBatchTaskStoreDO::getFinishedAt, finishedAt)
                .eq(EleStoreInventoryBatchTaskStoreDO::getTaskId, taskId)
                .in(EleStoreInventoryBatchTaskStoreDO::getStatus, "PENDING", "RUNNING"));
    }

    default int markRunningIfPending(Long taskStoreId, LocalDateTime startedAt) {
        return update(new LambdaUpdateWrapper<EleStoreInventoryBatchTaskStoreDO>()
                .set(EleStoreInventoryBatchTaskStoreDO::getStatus, "RUNNING")
                .set(EleStoreInventoryBatchTaskStoreDO::getStartedAt, startedAt)
                .eq(EleStoreInventoryBatchTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreInventoryBatchTaskStoreDO::getStatus, "PENDING"));
    }

    default int updateProgressIfRunning(Long taskStoreId, EleStoreInventoryBatchTaskStoreDO updateObj) {
        return update(new LambdaUpdateWrapper<EleStoreInventoryBatchTaskStoreDO>()
                .set(EleStoreInventoryBatchTaskStoreDO::getCurrentBatchNo, updateObj.getCurrentBatchNo())
                .set(EleStoreInventoryBatchTaskStoreDO::getTotalBatchNo, updateObj.getTotalBatchNo())
                .set(EleStoreInventoryBatchTaskStoreDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getFormalSuccessCount, updateObj.getFormalSuccessCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getShadowSuccessCount, updateObj.getShadowSuccessCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getFailureCount, updateObj.getFailureCount())
                .eq(EleStoreInventoryBatchTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreInventoryBatchTaskStoreDO::getStatus, "RUNNING"));
    }

    default int finishIfRunning(Long taskStoreId, String status, EleStoreInventoryBatchTaskStoreDO updateObj) {
        return update(new LambdaUpdateWrapper<EleStoreInventoryBatchTaskStoreDO>()
                .set(EleStoreInventoryBatchTaskStoreDO::getStatus, status)
                .set(EleStoreInventoryBatchTaskStoreDO::getCurrentBatchNo, updateObj.getCurrentBatchNo())
                .set(EleStoreInventoryBatchTaskStoreDO::getTotalBatchNo, updateObj.getTotalBatchNo())
                .set(EleStoreInventoryBatchTaskStoreDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getFormalSuccessCount, updateObj.getFormalSuccessCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getShadowSuccessCount, updateObj.getShadowSuccessCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getFailureCount, updateObj.getFailureCount())
                .set(EleStoreInventoryBatchTaskStoreDO::getErrorMsg, updateObj.getErrorMsg())
                .set(EleStoreInventoryBatchTaskStoreDO::getFinishedAt, updateObj.getFinishedAt())
                .eq(EleStoreInventoryBatchTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreInventoryBatchTaskStoreDO::getStatus, "RUNNING"));
    }
}
