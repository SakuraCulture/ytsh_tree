package cn.iocoder.yudao.module.ele.service.guard;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class StoreExecutionGuard {

    @Resource
    private EleOrderLockService eleOrderLockService;

    public void runWithStoreLock(StoreExecutionScenario scenario, String storeId, Runnable action) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario不能为空");
        }
        if (StrUtil.isBlank(storeId)) {
            throw new IllegalArgumentException("storeId不能为空");
        }
        if (action == null) {
            throw new IllegalArgumentException("action不能为空");
        }

        boolean acquired = tryLock(scenario, storeId);
        if (!acquired) {
            throw new IllegalStateException(scenario.getDescription() + "获取执行锁失败: " + storeId);
        }
        try {
            action.run();
        } finally {
            unlock(scenario, storeId);
        }
    }

    private boolean tryLock(StoreExecutionScenario scenario, String storeId) {
        return switch (scenario) {
            case STORE_GOODS -> eleOrderLockService.tryLockStoreGoodsExecution(
                    storeId, scenario.getWaitSeconds(), scenario.getLeaseMinutes());
            case STORE_INVENTORY -> eleOrderLockService.tryLockStoreInventoryExecution(
                    storeId, scenario.getWaitSeconds(), scenario.getLeaseMinutes());
        };
    }

    private void unlock(StoreExecutionScenario scenario, String storeId) {
        switch (scenario) {
            case STORE_GOODS -> eleOrderLockService.unlockStoreGoodsExecution(storeId);
            case STORE_INVENTORY -> eleOrderLockService.unlockStoreInventoryExecution(storeId);
        }
    }
}
