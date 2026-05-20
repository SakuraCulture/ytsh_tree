package cn.iocoder.yudao.module.ele.service.guard;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class StoreExecutionGuard {

    @Resource
    private EleOrderLockService eleOrderLockService;

    public void runWithStoreLock(StoreExecutionScenario scenario, String platformStoreId, Runnable action) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario不能为空");
        }
        if (StrUtil.isBlank(platformStoreId)) {
            throw new IllegalArgumentException("platformStoreId不能为空");
        }
        if (action == null) {
            throw new IllegalArgumentException("action不能为空");
        }

        boolean acquired = tryLock(scenario, platformStoreId);
        if (!acquired) {
            throw new IllegalStateException(scenario.getDescription() + "获取执行锁失败: " + platformStoreId);
        }
        try {
            action.run();
        } finally {
            unlock(scenario, platformStoreId);
        }
    }

    private boolean tryLock(StoreExecutionScenario scenario, String platformStoreId) {
        return switch (scenario) {
            case STORE_GOODS -> eleOrderLockService.tryLockStoreGoodsExecution(
                    platformStoreId, scenario.getWaitSeconds(), scenario.getLeaseMinutes());
            case STORE_INVENTORY -> eleOrderLockService.tryLockStoreInventoryExecution(
                    platformStoreId, scenario.getWaitSeconds(), scenario.getLeaseMinutes());
        };
    }

    private void unlock(StoreExecutionScenario scenario, String platformStoreId) {
        switch (scenario) {
            case STORE_GOODS -> eleOrderLockService.unlockStoreGoodsExecution(platformStoreId);
            case STORE_INVENTORY -> eleOrderLockService.unlockStoreInventoryExecution(platformStoreId);
        }
    }
}
