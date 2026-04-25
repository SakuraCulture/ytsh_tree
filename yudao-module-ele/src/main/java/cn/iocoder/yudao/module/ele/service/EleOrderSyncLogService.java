package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncLogRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncStatsRespVO;

public interface EleOrderSyncLogService {

    PageResult<EleOrderSyncLogRespVO> getSyncLogPage(String platformStoreId, String erpStoreCode,
                                                  Integer status, Long startTime, Long endTime,
                                                  Integer pageNo, Integer pageSize);

    EleOrderSyncLogRespVO getSyncLogById(Long id);

    EleOrderSyncStatsRespVO getStoreSyncStats(String platformStoreId);
}