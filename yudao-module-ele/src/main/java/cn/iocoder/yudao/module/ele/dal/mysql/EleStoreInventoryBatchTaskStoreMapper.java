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
}
