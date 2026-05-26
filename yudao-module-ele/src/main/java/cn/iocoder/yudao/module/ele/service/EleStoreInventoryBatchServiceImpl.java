package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchStoresReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStoreRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreInventoryBatchExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EleStoreInventoryBatchServiceImpl implements EleStoreInventoryBatchService {

    private static final Long ELE_PLATFORM_ID = 1L;
    private static final String SOURCE_TYPE_MANUAL = "MANUAL";
    private static final String SOURCE_TYPE_SCHEDULED = "SCHEDULED";
    private static final String SCOPE_CURRENT_STORE = "CURRENT_STORE";
    private static final String SCOPE_ALL_OPEN_STORES = "ALL_OPEN_STORES";
    private static final String SCOPE_SELECTED_STORES = "SELECTED_STORES";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int CREATE_LOCK_WAIT_SECONDS = 5;
    private static final int CREATE_LOCK_LEASE_MINUTES = 1;

    @Resource
    private EleStoreInventoryBatchTaskMapper taskMapper;
    @Resource
    private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Resource
    private StoreService storeService;
    @Resource
    private EleStoreInventoryBatchExecutor batchExecutor;
    @Resource
    private EleOrderLockService eleOrderLockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCurrentStoreBatchTask(EleStoreInventoryBatchCurrentReqVO reqVO) {
        String merchantCode = StrUtil.trim(reqVO.getMerchantCode());
        String erpStoreCode = StrUtil.trim(reqVO.getErpStoreCode());
        String platformStoreId = StrUtil.trim(reqVO.getPlatformStoreId());
        if (StrUtil.isBlank(platformStoreId)) {
            platformStoreId = erpStoreCode;
        }
        StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
        String lockKey = buildCurrentStoreTaskLockKey(erpStoreCode);
        eleOrderLockService.lockStoreInventoryBatchTask(lockKey, CREATE_LOCK_WAIT_SECONDS, CREATE_LOCK_LEASE_MINUTES);
        try {
            EleStoreInventoryBatchTaskDO runningTask = taskMapper.selectRunningCurrentStore(erpStoreCode);
            if (runningTask != null) {
                return runningTask.getId();
            }

            EleStoreInventoryBatchTaskDO task = createTask(SOURCE_TYPE_MANUAL, SCOPE_CURRENT_STORE, 1);
            taskMapper.insert(task);
            taskStoreMapper.insert(createCurrentTaskStore(task, merchantCode, erpStoreCode, platformStoreId, store));
            submitAndUnlockAfterTransaction(task.getId(), lockKey);
            return task.getId();
        } finally {
            unlockIfNoTransactionSynchronization(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAllOpenStoresBatchTask(EleStoreInventoryBatchAllOpenReqVO reqVO) {
        return createAllOpenStoresBatchTask(SOURCE_TYPE_MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScheduledAllOpenStoresBatchTask() {
        return createAllOpenStoresBatchTask(SOURCE_TYPE_SCHEDULED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStoresBatchTask(EleStoreInventoryBatchStoresReqVO reqVO) {
        List<String> platformStoreIds = reqVO.getPlatformStoreIds().stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(platformStoreIds)) {
            throw new IllegalArgumentException("platformStoreIds 不能为空");
        }

        String lockKey = buildSelectedStoresTaskLockKey();
        eleOrderLockService.lockStoreInventoryBatchTask(lockKey, CREATE_LOCK_WAIT_SECONDS, CREATE_LOCK_LEASE_MINUTES);
        try {
            List<StorePlatformRespVO> validStores = new ArrayList<>();
            for (String platformStoreId : platformStoreIds) {
                StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
                if (store != null) {
                    validStores.add(store);
                }
            }

            if (CollUtil.isEmpty(validStores)) {
                throw new RuntimeException("没有可拉取库存的有效门店");
            }

            EleStoreInventoryBatchTaskDO task = createTask(SOURCE_TYPE_MANUAL, SCOPE_SELECTED_STORES, validStores.size());
            taskMapper.insert(task);
            for (StorePlatformRespVO store : validStores) {
                taskStoreMapper.insert(createSelectedTaskStore(task, store));
            }
            submitAndUnlockAfterTransaction(task.getId(), lockKey);
            return task.getId();
        } finally {
            unlockIfNoTransactionSynchronization(lockKey);
        }
    }

    @Override
    public PageResult<EleStoreInventoryBatchTaskRespVO> getTaskPage(EleStoreInventoryBatchTaskPageReqVO reqVO) {
        return BeanUtils.toBean(taskMapper.selectPage(reqVO), EleStoreInventoryBatchTaskRespVO.class);
    }

    @Override
    public EleStoreInventoryBatchTaskRespVO getTask(Long id) {
        EleStoreInventoryBatchTaskDO task = taskMapper.selectById(id);
        return task == null ? null : BeanUtils.toBean(task, EleStoreInventoryBatchTaskRespVO.class);
    }

    @Override
    public PageResult<EleStoreInventoryBatchTaskStoreRespVO> getTaskStorePage(EleStoreInventoryBatchTaskStorePageReqVO reqVO) {
        return BeanUtils.toBean(taskStoreMapper.selectPage(reqVO), EleStoreInventoryBatchTaskStoreRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long id) {
        EleStoreInventoryBatchTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("库存批量任务不存在");
        }
        if (!STATUS_PENDING.equals(task.getStatus()) && !STATUS_RUNNING.equals(task.getStatus())) {
            throw new RuntimeException("只有待执行或执行中的任务可以取消");
        }
        LocalDateTime cancelledAt = LocalDateTime.now();
        EleStoreInventoryBatchTaskDO updateObj = new EleStoreInventoryBatchTaskDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_CANCELLED);
        updateObj.setFinishedAt(cancelledAt);
        taskMapper.updateById(updateObj);
        taskStoreMapper.cancelPendingByTaskId(id, cancelledAt);
    }

    private Long createAllOpenStoresBatchTask(String sourceType) {
        String lockKey = buildAllOpenStoresTaskLockKey();
        eleOrderLockService.lockStoreInventoryBatchTask(lockKey, CREATE_LOCK_WAIT_SECONDS, CREATE_LOCK_LEASE_MINUTES);
        try {
            EleStoreInventoryBatchTaskDO runningTask = taskMapper.selectRunningAllOpenStores();
            if (runningTask != null) {
                return runningTask.getId();
            }

            List<StorePlatformRespVO> stores = storeService.getOpenPlatformStores(ELE_PLATFORM_ID);
            if (CollUtil.isEmpty(stores)) {
                throw new RuntimeException("没有可拉取库存的饿了么开业门店");
            }

            EleStoreInventoryBatchTaskDO task = createTask(sourceType, SCOPE_ALL_OPEN_STORES, stores.size());
            taskMapper.insert(task);
            for (StorePlatformRespVO store : stores) {
                taskStoreMapper.insert(createOpenStoreTaskStore(task, store));
            }
            submitAndUnlockAfterTransaction(task.getId(), lockKey);
            return task.getId();
        } finally {
            unlockIfNoTransactionSynchronization(lockKey);
        }
    }

    private EleStoreInventoryBatchTaskDO createTask(String sourceType, String scope, int totalStoreCount) {
        EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
        task.setTaskNo(IdUtil.fastSimpleUUID());
        task.setSourceType(sourceType);
        task.setScope(scope);
        task.setStatus(STATUS_PENDING);
        task.setTotalStoreCount(totalStoreCount);
        task.setFinishedStoreCount(0);
        task.setTotalBatchCount(0);
        task.setFinishedBatchCount(0);
        task.setTotalSkuCount(0);
        task.setFormalSuccessCount(0);
        task.setShadowSuccessCount(0);
        task.setGovernanceCount(0);
        task.setFailureCount(0);
        return task;
    }

    private EleStoreInventoryBatchTaskStoreDO createCurrentTaskStore(EleStoreInventoryBatchTaskDO task,
                                                                     String merchantCode,
                                                                     String erpStoreCode,
                                                                     String platformStoreId,
                                                                     StorePlatformRespVO store) {
        EleStoreInventoryBatchTaskStoreDO taskStore = createTaskStore(task);
        taskStore.setMerchantCode(merchantCode);
        taskStore.setErpStoreCode(erpStoreCode);
        taskStore.setPlatformStoreId(platformStoreId);
        if (store != null) {
            taskStore.setStoreId(store.getStoreId());
            taskStore.setStoreName(store.getPlatformStoreName());
        }
        return taskStore;
    }

    private EleStoreInventoryBatchTaskStoreDO createOpenStoreTaskStore(EleStoreInventoryBatchTaskDO task,
                                                                       StorePlatformRespVO store) {
        String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
        EleStoreInventoryBatchTaskStoreDO taskStore = createTaskStore(task);
        taskStore.setStoreId(store.getStoreId());
        taskStore.setStoreName(store.getPlatformStoreName());
        taskStore.setMerchantCode(StrUtil.trim(store.getSettlementAccount()));
        taskStore.setErpStoreCode(platformStoreId);
        taskStore.setPlatformStoreId(platformStoreId);
        return taskStore;
    }

    private EleStoreInventoryBatchTaskStoreDO createSelectedTaskStore(EleStoreInventoryBatchTaskDO task,
                                                                       StorePlatformRespVO store) {
        String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
        EleStoreInventoryBatchTaskStoreDO taskStore = createTaskStore(task);
        taskStore.setStoreId(store.getStoreId());
        taskStore.setStoreName(store.getPlatformStoreName());
        taskStore.setMerchantCode(StrUtil.blankToDefault(StrUtil.trim(store.getSettlementAccount()), "LY_TT_QQD"));
        taskStore.setErpStoreCode(platformStoreId);
        taskStore.setPlatformStoreId(platformStoreId);
        return taskStore;
    }

    private EleStoreInventoryBatchTaskStoreDO createTaskStore(EleStoreInventoryBatchTaskDO task) {
        EleStoreInventoryBatchTaskStoreDO taskStore = new EleStoreInventoryBatchTaskStoreDO();
        taskStore.setTaskId(task.getId());
        taskStore.setTaskNo(task.getTaskNo());
        taskStore.setStatus(STATUS_PENDING);
        taskStore.setCurrentBatchNo(0);
        taskStore.setTotalBatchNo(0);
        taskStore.setTotalSkuCount(0);
        taskStore.setFormalSuccessCount(0);
        taskStore.setShadowSuccessCount(0);
        taskStore.setGovernanceCount(0);
        taskStore.setFailureCount(0);
        taskStore.setRetryCount(0);
        return taskStore;
    }

    private void submitAndUnlockAfterTransaction(Long taskId, String lockKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            batchExecutor.submit(taskId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                batchExecutor.submit(taskId);
            }

            @Override
            public void afterCompletion(int status) {
                eleOrderLockService.unlockStoreInventoryBatchTask(lockKey);
            }
        });
    }

    private void unlockIfNoTransactionSynchronization(String lockKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eleOrderLockService.unlockStoreInventoryBatchTask(lockKey);
        }
    }

    private String buildCurrentStoreTaskLockKey(String erpStoreCode) {
        return SCOPE_CURRENT_STORE + ":" + erpStoreCode;
    }

    private String buildAllOpenStoresTaskLockKey() {
        return SCOPE_ALL_OPEN_STORES;
    }

    private String buildSelectedStoresTaskLockKey() {
        return SCOPE_SELECTED_STORES;
    }
}
