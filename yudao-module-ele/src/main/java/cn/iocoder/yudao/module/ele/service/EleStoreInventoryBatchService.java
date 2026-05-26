package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchStoresReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStoreRespVO;

public interface EleStoreInventoryBatchService {

    Long createCurrentStoreBatchTask(EleStoreInventoryBatchCurrentReqVO reqVO);

    Long createAllOpenStoresBatchTask(EleStoreInventoryBatchAllOpenReqVO reqVO);

    Long createScheduledAllOpenStoresBatchTask();

    Long createStoresBatchTask(EleStoreInventoryBatchStoresReqVO reqVO);

    PageResult<EleStoreInventoryBatchTaskRespVO> getTaskPage(EleStoreInventoryBatchTaskPageReqVO reqVO);

    EleStoreInventoryBatchTaskRespVO getTask(Long id);

    PageResult<EleStoreInventoryBatchTaskStoreRespVO> getTaskStorePage(EleStoreInventoryBatchTaskStorePageReqVO reqVO);

    void cancelTask(Long id);
}
