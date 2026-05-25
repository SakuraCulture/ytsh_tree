package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;

import java.util.List;

public interface EleStoreGoodsFullSyncExecutor {

    void submit(Long taskId);

    void execute(Long taskId);

    void submitDirectly(List<StorePlatformRespVO> stores, Boolean testMode);

    void executeDirectly(List<StorePlatformRespVO> stores, Boolean testMode);
}
