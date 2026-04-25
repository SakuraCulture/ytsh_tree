package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import org.apache.ibatis.annotations.Mapper;

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
