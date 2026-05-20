package cn.iocoder.yudao.module.ele.service.guard;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreExecutionGuardTest extends BaseMockitoUnitTest {

    @InjectMocks
    private StoreExecutionGuard storeExecutionGuard;

    @Mock
    private EleOrderLockService eleOrderLockService;

    @Test
    void testRunWithStoreLock_whenStoreGoodsLockAcquired_thenRunAndUnlock() {
        AtomicBoolean executed = new AtomicBoolean(false);
        when(eleOrderLockService.tryLockStoreGoodsExecution("platform-store-1",
                StoreExecutionScenario.STORE_GOODS.getWaitSeconds(),
                StoreExecutionScenario.STORE_GOODS.getLeaseMinutes())).thenReturn(true);

        storeExecutionGuard.runWithStoreLock(StoreExecutionScenario.STORE_GOODS,
                "platform-store-1", () -> executed.set(true));

        assertTrue(executed.get());
        verify(eleOrderLockService).unlockStoreGoodsExecution("platform-store-1");
    }

    @Test
    void testRunWithStoreLock_whenStoreInventoryLockNotAcquired_thenThrowException() {
        when(eleOrderLockService.tryLockStoreInventoryExecution("platform-store-2",
                StoreExecutionScenario.STORE_INVENTORY.getWaitSeconds(),
                StoreExecutionScenario.STORE_INVENTORY.getLeaseMinutes())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> storeExecutionGuard.runWithStoreLock(
                StoreExecutionScenario.STORE_INVENTORY, "platform-store-2", () -> {
                }));

        verify(eleOrderLockService, never()).unlockStoreInventoryExecution("platform-store-2");
    }

    @Test
    void testRunWithStoreLock_whenActionThrows_thenStillUnlock() {
        when(eleOrderLockService.tryLockStoreGoodsExecution("platform-store-3",
                StoreExecutionScenario.STORE_GOODS.getWaitSeconds(),
                StoreExecutionScenario.STORE_GOODS.getLeaseMinutes())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> storeExecutionGuard.runWithStoreLock(
                StoreExecutionScenario.STORE_GOODS, "platform-store-3", () -> {
                    throw new IllegalStateException("boom");
                }));

        verify(eleOrderLockService).unlockStoreGoodsExecution("platform-store-3");
    }

    @Test
    void testRunWithStoreLock_whenPlatformStoreIdBlank_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> storeExecutionGuard.runWithStoreLock(
                StoreExecutionScenario.STORE_GOODS, " ", () -> {
                }));
    }
}
