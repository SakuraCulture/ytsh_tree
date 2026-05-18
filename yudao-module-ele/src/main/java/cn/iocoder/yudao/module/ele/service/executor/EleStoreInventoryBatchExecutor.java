package cn.iocoder.yudao.module.ele.service.executor;

public interface EleStoreInventoryBatchExecutor {

    void submit(Long taskId);

    void execute(Long taskId);
}
