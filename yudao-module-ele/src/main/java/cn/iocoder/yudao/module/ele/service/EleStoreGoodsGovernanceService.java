package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsGovernancePoolDO;

public interface EleStoreGoodsGovernanceService {

    Long create(EleStoreGoodsGovernancePoolDO governancePool);

    PageResult<EleStoreGoodsGovernancePoolRespVO> getPage(EleStoreGoodsGovernancePoolPageReqVO reqVO);

    EleStoreGoodsGovernancePoolRespVO getById(Long id);

    void markProcessed(Long id, String remark);

    void markIgnored(Long id, String remark);
}
