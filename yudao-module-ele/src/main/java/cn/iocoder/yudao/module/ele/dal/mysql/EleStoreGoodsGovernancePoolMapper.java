package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsGovernancePoolDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleStoreGoodsGovernancePoolMapper extends BaseMapperX<EleStoreGoodsGovernancePoolDO> {

    default EleStoreGoodsGovernancePoolDO selectByIdAndErpStoreCode(Long id, String erpStoreCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsGovernancePoolDO>()
                .eq(EleStoreGoodsGovernancePoolDO::getId, id)
                .eq(EleStoreGoodsGovernancePoolDO::getErpStoreCode, erpStoreCode));
    }

    default List<EleStoreGoodsGovernancePoolDO> selectPendingList() {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsGovernancePoolDO>()
                .eq(EleStoreGoodsGovernancePoolDO::getProcessStatus, "PENDING")
                .orderByAsc(EleStoreGoodsGovernancePoolDO::getCreateTime));
    }

    default PageResult<EleStoreGoodsGovernancePoolDO> selectPage(EleStoreGoodsGovernancePoolPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreGoodsGovernancePoolDO>()
                .likeIfPresent(EleStoreGoodsGovernancePoolDO::getMerchantCode, reqVO.getMerchantCode())
                .likeIfPresent(EleStoreGoodsGovernancePoolDO::getErpStoreCode, reqVO.getErpStoreCode())
                .likeIfPresent(EleStoreGoodsGovernancePoolDO::getPlatformStoreId, reqVO.getPlatformStoreId())
                .likeIfPresent(EleStoreGoodsGovernancePoolDO::getSkuCode, reqVO.getSkuCode())
                .likeIfPresent(EleStoreGoodsGovernancePoolDO::getSpuCode, reqVO.getSpuCode())
                .eqIfPresent(EleStoreGoodsGovernancePoolDO::getReasonCode, reqVO.getReasonCode())
                .eqIfPresent(EleStoreGoodsGovernancePoolDO::getProcessStatus, reqVO.getProcessStatus())
                .orderByDesc(EleStoreGoodsGovernancePoolDO::getCreateTime));
    }
}
