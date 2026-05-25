package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStoreRespVO;

public interface EleStoreGoodsFullSyncService {

    Long createCurrentStoreFullSync(EleStoreGoodsFullSyncCurrentReqVO reqVO);

    Boolean createAllOpenStoresFullSync(EleStoreGoodsFullSyncAllOpenReqVO reqVO);

    PageResult<EleStoreGoodsFullSyncTaskRespVO> getTaskPage(EleStoreGoodsFullSyncTaskPageReqVO reqVO);

    EleStoreGoodsFullSyncTaskRespVO getTask(Long id);

    PageResult<EleStoreGoodsFullSyncTaskStoreRespVO> getTaskStorePage(EleStoreGoodsFullSyncTaskStorePageReqVO reqVO);

    void cancelTask(Long id);
}
