package cn.iocoder.yudao.module.ele.service.executor;

public interface EleStoreGoodsFullSyncExecutor {

    void submit(Long taskId);

    void execute(Long taskId);
}
