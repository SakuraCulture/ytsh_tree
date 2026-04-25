package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsSyncLogRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsSyncLogDO;

public interface EleStoreGoodsSyncLogService {

    Long create(EleStoreGoodsSyncLogDO syncLog);

    PageResult<EleStoreGoodsSyncLogRespVO> getSyncLogPage(String platformStoreId, String erpStoreCode,
                                                          String skuCode, Boolean success,
                                                          Integer pageNo, Integer pageSize);

    EleStoreGoodsSyncLogRespVO getSyncLogById(Long id);
}
