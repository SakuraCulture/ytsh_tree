package cn.iocoder.yudao.module.ele.service.guard;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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

        long lockStart = System.currentTimeMillis();
        log.info("[门店执行锁] 开始获取: scenario={}, platformStoreId={}, waitSeconds={}, leaseMinutes={}",
                scenario.getDescription(), platformStoreId, scenario.getWaitSeconds(), scenario.getLeaseMinutes());
        boolean acquired = tryLock(scenario, platformStoreId);
        log.info("[门店执行锁] 获取结果: scenario={}, platformStoreId={}, acquired={}, 耗时={}ms",
                scenario.getDescription(), platformStoreId, acquired, System.currentTimeMillis() - lockStart);
        if (!acquired) {
            throw new IllegalStateException(scenario.getDescription() + "获取执行锁失败: " + platformStoreId);
        }
        try {
            action.run();
        } finally {
            unlock(scenario, platformStoreId);
            log.info("[门店执行锁] 已释放: scenario={}, platformStoreId={}",
                    scenario.getDescription(), platformStoreId);
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
