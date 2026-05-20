package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EleStoreGoodsFullSyncTaskStoreMapper extends BaseMapperX<EleStoreGoodsFullSyncTaskStoreDO> {

    default List<EleStoreGoodsFullSyncTaskStoreDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskStoreDO>()
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getTaskId, taskId)
                .orderByAsc(EleStoreGoodsFullSyncTaskStoreDO::getId));
    }

    default PageResult<EleStoreGoodsFullSyncTaskStoreDO> selectPage(EleStoreGoodsFullSyncTaskStorePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreGoodsFullSyncTaskStoreDO>()
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(EleStoreGoodsFullSyncTaskStoreDO::getStatus, reqVO.getStatus())
                .likeIfPresent(EleStoreGoodsFullSyncTaskStoreDO::getErpStoreCode, reqVO.getErpStoreCode())
                .likeIfPresent(EleStoreGoodsFullSyncTaskStoreDO::getStoreId, reqVO.getStoreId())
                .orderByAsc(EleStoreGoodsFullSyncTaskStoreDO::getId));
    }

    default int cancelPendingByTaskId(Long taskId, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskStoreDO>()
                .set(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "CANCELLED")
                .set(EleStoreGoodsFullSyncTaskStoreDO::getFinishedAt, finishedAt)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getTaskId, taskId)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "PENDING"));
    }

    default int failUnfinishedByTaskId(Long taskId, String errorMsg, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskStoreDO>()
                .set(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "FAILED")
                .set(EleStoreGoodsFullSyncTaskStoreDO::getErrorMsg, errorMsg)
                .set(EleStoreGoodsFullSyncTaskStoreDO::getFinishedAt, finishedAt)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getTaskId, taskId)
                .in(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "PENDING", "RUNNING"));
    }

    default int markRunningIfPending(Long taskStoreId, LocalDateTime startedAt) {
        return update(new LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskStoreDO>()
                .set(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "RUNNING")
                .set(EleStoreGoodsFullSyncTaskStoreDO::getStartedAt, startedAt)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "PENDING"));
    }

    default int updateProgressIfRunning(Long taskStoreId, EleStoreGoodsFullSyncTaskStoreDO updateObj) {
        return update(new LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskStoreDO>()
                .set(EleStoreGoodsFullSyncTaskStoreDO::getCurrentPage, updateObj.getCurrentPage())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getTotalPage, updateObj.getTotalPage())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getPageSize, updateObj.getPageSize())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getSuccessCount, updateObj.getSuccessCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getFailCount, updateObj.getFailCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getGovernanceCount, updateObj.getGovernanceCount())
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "RUNNING"));
    }

    default int finishIfRunning(Long taskStoreId, String status, EleStoreGoodsFullSyncTaskStoreDO updateObj) {
        return update(new LambdaUpdateWrapper<EleStoreGoodsFullSyncTaskStoreDO>()
                .set(EleStoreGoodsFullSyncTaskStoreDO::getStatus, status)
                .set(EleStoreGoodsFullSyncTaskStoreDO::getCurrentPage, updateObj.getCurrentPage())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getTotalPage, updateObj.getTotalPage())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getTotalSkuCount, updateObj.getTotalSkuCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getSuccessCount, updateObj.getSuccessCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getFailCount, updateObj.getFailCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getGovernanceCount, updateObj.getGovernanceCount())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getErrorMsg, updateObj.getErrorMsg())
                .set(EleStoreGoodsFullSyncTaskStoreDO::getFinishedAt, updateObj.getFinishedAt())
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getId, taskStoreId)
                .eq(EleStoreGoodsFullSyncTaskStoreDO::getStatus, "RUNNING"));
    }
}
