package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStoreRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreGoodsFullSyncExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EleStoreGoodsFullSyncServiceImpl implements EleStoreGoodsFullSyncService {

    private static final Long ELE_PLATFORM_ID = 1L;
    private static final String SCOPE_CURRENT_STORE = "CURRENT_STORE";
    private static final String SCOPE_ALL_OPEN_STORES = "ALL_OPEN_STORES";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Integer DEFAULT_PAGE_SIZE = 20;
    private static final int CREATE_LOCK_WAIT_SECONDS = 5;
    private static final int CREATE_LOCK_LEASE_MINUTES = 1;

    @Resource
    private EleStoreGoodsFullSyncTaskMapper taskMapper;
    @Resource
    private EleStoreGoodsFullSyncTaskStoreMapper taskStoreMapper;
    @Resource
    private StoreService storeService;
    @Resource
    private EleStoreGoodsFullSyncExecutor fullSyncExecutor;
    @Resource
    private EleOrderLockService eleOrderLockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCurrentStoreFullSync(EleStoreGoodsFullSyncCurrentReqVO reqVO) {
        String merchantCode = StrUtil.trim(reqVO.getMerchantCode());
        String erpStoreCode = StrUtil.trim(reqVO.getErpStoreCode());
        String lockKey = buildCurrentStoreTaskLockKey(erpStoreCode);
        eleOrderLockService.lockStoreGoodsFullSyncTask(lockKey, CREATE_LOCK_WAIT_SECONDS, CREATE_LOCK_LEASE_MINUTES);
        try {
            EleStoreGoodsFullSyncTaskDO runningTask = taskMapper.selectRunningCurrentStore(erpStoreCode);
            if (runningTask != null) {
                return runningTask.getId();
            }

            EleStoreGoodsFullSyncTaskDO task = createTask(SCOPE_CURRENT_STORE, merchantCode, erpStoreCode,
                    Boolean.TRUE.equals(reqVO.getTestMode()), 1);
            taskMapper.insert(task);
            taskStoreMapper.insert(createCurrentTaskStore(task, merchantCode, erpStoreCode));
            submitAndUnlockAfterTransaction(task.getId(), lockKey);
            return task.getId();
        } finally {
            unlockIfNoTransactionSynchronization(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAllOpenStoresFullSync(EleStoreGoodsFullSyncAllOpenReqVO reqVO) {
        String lockKey = buildAllOpenStoresTaskLockKey();
        eleOrderLockService.lockStoreGoodsFullSyncTask(lockKey, CREATE_LOCK_WAIT_SECONDS, CREATE_LOCK_LEASE_MINUTES);
        try {
            EleStoreGoodsFullSyncTaskDO runningTask = taskMapper.selectRunningAllOpenStores();
            if (runningTask != null) {
                return runningTask.getId();
            }

            List<StorePlatformRespVO> stores = storeService.getOpenPlatformStores(ELE_PLATFORM_ID);
            if (CollUtil.isEmpty(stores)) {
                throw new RuntimeException("没有可同步的饿了么开业门店");
            }

            EleStoreGoodsFullSyncTaskDO task = createTask(SCOPE_ALL_OPEN_STORES, null, null,
                    reqVO != null && Boolean.TRUE.equals(reqVO.getTestMode()), stores.size());
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

    @Override
    public PageResult<EleStoreGoodsFullSyncTaskRespVO> getTaskPage(EleStoreGoodsFullSyncTaskPageReqVO reqVO) {
        return BeanUtils.toBean(taskMapper.selectPage(reqVO), EleStoreGoodsFullSyncTaskRespVO.class);
    }

    @Override
    public EleStoreGoodsFullSyncTaskRespVO getTask(Long id) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(id);
        return task == null ? null : BeanUtils.toBean(task, EleStoreGoodsFullSyncTaskRespVO.class);
    }

    @Override
    public PageResult<EleStoreGoodsFullSyncTaskStoreRespVO> getTaskStorePage(EleStoreGoodsFullSyncTaskStorePageReqVO reqVO) {
        return BeanUtils.toBean(taskStoreMapper.selectPage(reqVO), EleStoreGoodsFullSyncTaskStoreRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long id) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("全量同步任务不存在");
        }
        if (!STATUS_PENDING.equals(task.getStatus()) && !STATUS_RUNNING.equals(task.getStatus())) {
            throw new RuntimeException("只有待执行或执行中的任务可以取消");
        }
        LocalDateTime cancelledAt = LocalDateTime.now();
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_CANCELLED);
        updateObj.setFinishedAt(cancelledAt);
        taskMapper.updateById(updateObj);
        taskStoreMapper.cancelPendingByTaskId(id, cancelledAt);
    }

    private EleStoreGoodsFullSyncTaskDO createTask(String scope, String merchantCode, String erpStoreCode,
                                                  boolean testMode, int totalStoreCount) {
        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setTaskNo(IdUtil.fastSimpleUUID());
        task.setScope(scope);
        task.setMerchantCode(merchantCode);
        task.setErpStoreCode(erpStoreCode);
        task.setTestMode(testMode);
        task.setStatus(STATUS_PENDING);
        task.setTotalStoreCount(totalStoreCount);
        task.setFinishedStoreCount(0);
        task.setTotalPageCount(0);
        task.setFinishedPageCount(0);
        task.setTotalSkuCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setGovernanceCount(0);
        return task;
    }

    private EleStoreGoodsFullSyncTaskStoreDO createCurrentTaskStore(EleStoreGoodsFullSyncTaskDO task,
                                                                   String merchantCode, String erpStoreCode) {
        EleStoreGoodsFullSyncTaskStoreDO taskStore = createTaskStore(task);
        taskStore.setMerchantCode(merchantCode);
        taskStore.setErpStoreCode(erpStoreCode);
        taskStore.setPlatformStoreId(erpStoreCode);
        return taskStore;
    }

    private EleStoreGoodsFullSyncTaskStoreDO createOpenStoreTaskStore(EleStoreGoodsFullSyncTaskDO task,
                                                                     StorePlatformRespVO store) {
        String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
        EleStoreGoodsFullSyncTaskStoreDO taskStore = createTaskStore(task);
        taskStore.setStoreId(store.getStoreId());
        taskStore.setStoreName(store.getPlatformStoreName());
        taskStore.setMerchantCode(StrUtil.trim(store.getSettlementAccount()));
        taskStore.setErpStoreCode(platformStoreId);
        taskStore.setPlatformStoreId(platformStoreId);
        return taskStore;
    }

    private EleStoreGoodsFullSyncTaskStoreDO createTaskStore(EleStoreGoodsFullSyncTaskDO task) {
        EleStoreGoodsFullSyncTaskStoreDO taskStore = new EleStoreGoodsFullSyncTaskStoreDO();
        taskStore.setTaskId(task.getId());
        taskStore.setTaskNo(task.getTaskNo());
        taskStore.setStatus(STATUS_PENDING);
        taskStore.setCurrentPage(0);
        taskStore.setTotalPage(0);
        taskStore.setPageSize(DEFAULT_PAGE_SIZE);
        taskStore.setTotalSkuCount(0);
        taskStore.setSuccessCount(0);
        taskStore.setFailCount(0);
        taskStore.setGovernanceCount(0);
        taskStore.setRetryCount(0);
        return taskStore;
    }

    private void submitAndUnlockAfterTransaction(Long taskId, String lockKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fullSyncExecutor.submit(taskId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fullSyncExecutor.submit(taskId);
            }

            @Override
            public void afterCompletion(int status) {
                eleOrderLockService.unlockStoreGoodsFullSyncTask(lockKey);
            }
        });
    }

    private void unlockIfNoTransactionSynchronization(String lockKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eleOrderLockService.unlockStoreGoodsFullSyncTask(lockKey);
        }
    }

    private String buildCurrentStoreTaskLockKey(String erpStoreCode) {
        return SCOPE_CURRENT_STORE + ":" + erpStoreCode;
    }

    private String buildAllOpenStoresTaskLockKey() {
        return SCOPE_ALL_OPEN_STORES;
    }
}
