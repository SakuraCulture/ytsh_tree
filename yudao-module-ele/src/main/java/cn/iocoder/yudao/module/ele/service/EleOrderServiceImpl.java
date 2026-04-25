package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderFailRecordRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderStatusLog;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDiscountDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderItemDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderPlatformDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrder;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderFailRecordMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderStatusLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderSyncLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderDiscountMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderItemMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderPlatformMapper;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import cn.iocoder.yudao.module.ele.enums.EleDeliveryStatusEnum;
import cn.iocoder.yudao.module.ele.mq.EleOrderKafkaProducer;
import cn.iocoder.yudao.module.ele.enums.EleOrderStatusEnum;
import cn.iocoder.yudao.module.ele.service.dto.EleCompensateProgressDTO;
import cn.iocoder.yudao.module.ele.service.dto.EleSyncSubmitRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import cn.iocoder.yudao.module.ele.util.RetryUtil;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor;
import cn.iocoder.yudao.module.ele.exception.EleOrderSyncException;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.traffic.EleTrafficInterceptor;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.redisson.RedissonShutdownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lib.ele.retail.param.MeEleRetailSaasOrderListDetailResDto;
import lib.ele.retail.param.MeEleRetailSaasOrderListReqDto;
import lib.ele.retail.param.MeEleRetailSaasOrderListResDto;
import lib.ele.retail.param.SaasOrderGetParam;
import lib.ele.retail.param.SaasOrderGetResult;
import lib.ele.retail.param.SaasOrderListParam;
import lib.ele.retail.param.SaasOrderListResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.annotation.PreDestroy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.RateLimiter;

@Service
public class EleOrderServiceImpl implements EleOrderService {

    private static final Logger log = LoggerFactory.getLogger(EleOrderServiceImpl.class);

    private static final String PLATFORM = "ELE";
    private static final Map<String, EleCompensateProgressDTO> PROGRESS_CACHE = new ConcurrentHashMap<>();

    private static final RateLimiter ORDER_LIST_RATE_LIMITER = RateLimiter.create(400);
    private static final RateLimiter ORDER_GET_RATE_LIMITER = RateLimiter.create(200);

    @Resource
    private EleOrderSyncTaskExecutor syncTaskExecutor;

    @Resource
    private EleApiConfigMapper eleApiConfigMapper;
    @Resource
    private StoreService storeService;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderPlatformMapper orderPlatformMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private OrderDiscountMapper orderDiscountMapper;
    @Resource
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;
    @Resource
    private EleOrderFailRecordMapper eleOrderFailRecordMapper;
    @Resource
    private EleOrderMapper eleOrderMapper;
    @Resource
    private EleOrderStatusLogMapper eleOrderStatusLogMapper;
    @Resource
    private EleOrderKafkaProducer eleOrderKafkaProducer;
    @Resource
    private EleOrderConvertService eleOrderConvertService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private EleOrderLockService eleOrderLockService;
    @Resource(name = "eleOrderCompensateExecutor")
    private ThreadPoolTaskExecutor compensateExecutor;
    @Resource
    private EleOrderRetryTaskSubmitter retryTaskSubmitter;
    @Resource
    private EleOpenApiClient eleOpenApiClient;
    @Resource
    private ShutdownStateManager shutdownStateManager;

    @PostConstruct
    public void initSyncDelegate() {
        syncTaskExecutor.setSyncDelegate(
                (store, forcedStartTime, forcedEndTime) -> syncOrdersWithWindow(store, forcedStartTime, forcedEndTime));
    }

    @Override
    public OrderListRespDTO getOrderList(OrderListReqDTO req) {
        EleApiConfig config = getApiConfig();

        String platformStoreId = StrUtil.trim(req.getPlatformStoreId());
        String merchantCode = StrUtil.trim(req.getMerchantCode());
        String erpStoreCode = StrUtil.isNotBlank(platformStoreId)
                ? platformStoreId
                : StrUtil.trim(req.getErpStoreCode());

        if (StrUtil.isBlank(merchantCode)) {
            merchantCode = StrUtil.trim(config.getMerchantCode());
        }

        if (StrUtil.isBlank(merchantCode)) {
            throw new RuntimeException("merchantCode不能为空");
        }
        if (StrUtil.isBlank(erpStoreCode)) {
            throw new RuntimeException("erpStoreCode不能为空");
        }

        Long startTime = req.getStartTime();
        if (startTime != null && startTime > 100000000000L) {
            startTime = startTime / 1000;
        }
        Long endTime = req.getEndTime();
        if (endTime != null && endTime > 100000000000L) {
            endTime = endTime / 1000;
        }
        if (startTime == null && endTime == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startTime = calendar.getTimeInMillis() / 1000;
            endTime = System.currentTimeMillis() / 1000;
            log.info("【默认时间范围】未传入时间参数，默认查询当日订单，startTime={}, endTime={}", startTime, endTime);
        }

        MeEleRetailSaasOrderListReqDto body = new MeEleRetailSaasOrderListReqDto();
        body.setMerchant_code(merchantCode);
        body.setErp_store_code(erpStoreCode);
        if (req.getStatus() != null) {
            body.setStatus(req.getStatus());
        }
        body.setStart_time(startTime);
        body.setEnd_time(endTime);
        body.setPage_size(req.getPageSize() != null ? req.getPageSize() : 20);
        if (req.getScrollId() != null) {
            body.setScroll_id(req.getScrollId());
        }

        SaasOrderListParam param = new SaasOrderListParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());
        param.setEncrypt("aes");
        param.setBody(body);

        log.info(
                "【翱象订单列表请求】platformStoreId={}, merchantCode={}, erpStoreCode={}, status={}, startTime={}, endTime={}, pageSize={}, scrollId={}",
                platformStoreId, merchantCode, erpStoreCode, req.getStatus(), startTime, endTime,
                req.getPageSize() != null ? req.getPageSize() : 20, req.getScrollId());

        ORDER_GET_RATE_LIMITER.acquire();
        try {
            BizResultWrapper<SaasOrderListResult> wrapper = eleOpenApiClient.sendOrderList(config, param, merchantCode,
                    platformStoreId, erpStoreCode);
            OrderListRespDTO listResult = convertListResult(wrapper);

            int orderCount = listResult != null && listResult.getOrderList() != null ? listResult.getOrderList().size()
                    : 0;
            log.info(
                    "【翱象订单列表响应】platformStoreId={}, merchantCode={}, erpStoreCode={}, status={}, orderCount={}, scrollId={}",
                    platformStoreId, merchantCode, erpStoreCode, req.getStatus(), orderCount,
                    listResult == null ? null : listResult.getScrollId());

            if (listResult != null && CollUtil.isNotEmpty(listResult.getOrderList())) {
                log.info("【翱象API】merchantCode={}, erpStoreCode={}, 查找到{}条订单", merchantCode, erpStoreCode,
                        listResult.getOrderList().size());
                for (OrderListRespDTO.OrderDetail order : listResult.getOrderList()) {
                    log.info("【翱象API】订单完整信息: orderId={}, channelOrderId={}, status={}, deliveryStatus={}, " +
                            "totalFee={}, payFee={}, discountFee={}, deliveryFee={}, packageFee={}, " +
                            "channelSourceName={}, buyerName={}, buyerPhone={}, buyerAddress={}, " +
                            "deliveryName={}, deliveryPhone={}, deliveryPlatform={}, deliveryType={}, " +
                            "createTime={}, payTime={}",
                            order.getOrderId(), order.getChannelOrderId(), order.getStatus(), order.getDeliveryStatus(),
                            order.getTotalFee(), order.getPayFee(), order.getDiscountFee(), order.getDeliveryFee(),
                            order.getPackageFee(), order.getChannelSourceName(), order.getBuyerName(),
                            order.getBuyerPhone(), order.getBuyerAddress(), order.getDeliveryName(),
                            order.getDeliveryPhone(), order.getDeliveryPlatform(), order.getDeliveryType(),
                            order.getCreateTime(), order.getPayTime());
                }
            } else {
                log.info("【翱象API】merchantCode={}, erpStoreCode={}, 未查到订单", merchantCode, erpStoreCode);
            }

            enrichOrderListWithDetails(listResult, req, merchantCode, erpStoreCode);
            return listResult;
        } catch (Exception e) {
            log.error("【翱象订单列表查询失败】platformStoreId={}, merchantCode={}, erpStoreCode={}, error={}",
                    platformStoreId, merchantCode, erpStoreCode, e.getMessage(), e);
            saveFailRecord(null, null, "SYNC", "API", e.getMessage(), req, null, 0, null, platformStoreId, merchantCode,
                    erpStoreCode);
            OrderListRespDTO emptyResult = new OrderListRespDTO();
            emptyResult.setTotal(0L);
            emptyResult.setOrderList(new ArrayList<>());
            return emptyResult;
        }
    }

    @Override
    public OrderDetailRespDTO getOrderDetail(String platformStoreId, String merchantCode, String erpStoreCode,
            String orderId) {
        OrderDetailRespDTO local = getDetailFromLocal(orderId);
        return local != null ? local : getOrderDetailRemote(platformStoreId, merchantCode, erpStoreCode, orderId);
    }

    // @Scheduled(cron = "0 */60 * * * ?")
    public void syncOrdersHourly() {
        runSyncCycle(null, null);
    }

    @Override
    public void scanPendingRetryRecords() {
        long startTime = System.currentTimeMillis();
        try {
            List<EleOrderFailRecord> pendingRecords = eleOrderFailRecordMapper.selectList(
                    new LambdaQueryWrapperX<EleOrderFailRecord>()
                            .eq(EleOrderFailRecord::getProcessStatus, "PENDING_RETRY")
                            .lt(EleOrderFailRecord::getRetryCount, 3)
                            .orderByAsc(EleOrderFailRecord::getCreateTime)
                            .last("LIMIT 100"));

            if (pendingRecords == null || pendingRecords.isEmpty()) {
                return;
            }

            log.info("【定时扫描】发现{}条PENDING_RETRY记录，准备重新提交", pendingRecords.size());

            for (EleOrderFailRecord record : pendingRecords) {
                try {
                    String platformStoreId = record.getPlatformStoreId();
                    if (platformStoreId == null || platformStoreId.isEmpty()) {
                        log.warn("【定时扫描】失败记录缺少platformStoreId，跳过，orderId={}, recordId={}",
                                record.getOrderId(), record.getId());
                        continue;
                    }

                    StorePlatformRespVO platformInfo = storeService
                            .getPlatformTableByPlatformStoreId(platformStoreId);
                    if (platformInfo == null) {
                        log.warn("【定时扫描】无法获取门店信息，跳过，orderId={}, platformStoreId={}",
                                record.getOrderId(), platformStoreId);
                        continue;
                    }

                    EleOrderRetryTaskSubmitter.RetryTask task = new EleOrderRetryTaskSubmitter.RetryTask(
                            record.getOrderId(), record.getChannelOrderId(),
                            platformInfo.getPlatformStoreId(),
                            platformInfo.getSettlementAccount(),
                            platformInfo.getPlatformStoreId(),
                            record.getId(), null);

                    List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
                    tasks.add(task);
                    retryTaskSubmitter.submitRetryTasks(tasks);

                } catch (Exception e) {
                    log.warn("【定时扫描】提交重试任务异常，orderId={}, error={}",
                            record.getOrderId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("【定时扫描】扫描PENDING_RETRY记录异常", e);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【定时扫描】执行完成，耗时: {} ms", elapsed);
        }
    }

    @Override
    public void syncOrders(String platformStoreId, String merchantCode, String erpStoreCode) {
        syncOrders(platformStoreId, merchantCode, erpStoreCode, null, null);
    }

    @Override
    public void syncOrders(String platformStoreId, String merchantCode, String erpStoreCode, Long startTime,
            Long endTime) {
        boolean locked = eleOrderLockService.tryLockSync(platformStoreId, 1, 5);
        if (!locked) {
            throw new RuntimeException("门店同步任务正在执行中: " + platformStoreId);
        }
        try {
            StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
            if (store == null) {
                throw new RuntimeException("未找到门店信息: " + platformStoreId);
            }
            syncOrdersWithWindow(store, startTime, endTime);
        } finally {
            eleOrderLockService.unlockSync(platformStoreId);
        }
    }

    private void syncOrdersWithWindow(StorePlatformRespVO store, Long forcedStartTime, Long forcedEndTime) {
        shutdownStateManager.registerStoreSyncStarted(
                StrUtil.trim(store.getPlatformStoreId()), store.getPlatformStoreName());
        try {
            if (shutdownStateManager.isShuttingDown()) {
                log.warn("【订单同步】应用正在关闭，跳过门店同步，platformStoreId={}", store.getPlatformStoreId());
                throw new EleOrderSyncException("【订单同步】应用正在关闭，跳过门店同步，platformStoreId=" + store.getPlatformStoreId());
            }

            String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
            String merchantCode = StrUtil.trim(store.getSettlementAccount());
            String erpStoreCode = platformStoreId;
            String storeName = store.getPlatformStoreName();
            LocalDateTime syncStartTime = LocalDateTime.now();

            Long startTime = forcedStartTime;
            if (startTime != null && startTime > 100000000000L) {
                startTime = startTime / 1000;
            }

            if (startTime == null) {
                EleOrderSyncLog lastSync = eleOrderSyncLogMapper.selectLastSync(platformStoreId);
                if (lastSync != null && lastSync.getSyncTime() != null) {
                    startTime = lastSync.getSyncTime();
                    if (startTime > 100000000000L) {
                        startTime = startTime / 1000;
                    }
                    log.info("【增量同步】门店{}，从上次同步时间继续，startTime={}", platformStoreId, startTime);
                }
                if (startTime == null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis() / 1000;
                    log.info("【首次同步】门店{}无同步记录，从本月1日0点起拉取订单，startTime={}", platformStoreId, startTime);
                }
            }

            Long endTime = forcedEndTime;
            if (endTime != null && endTime > 100000000000L) {
                endTime = endTime / 1000;
            }
            if (endTime == null) {
                endTime = System.currentTimeMillis() / 1000;
            }

            try {
                List<OrderListRespDTO.OrderDetail> allOrders = pullAllOrders(platformStoreId, merchantCode,
                        erpStoreCode,
                        startTime, endTime);

                Long lastSuccessTimestamp = saveOrUpdateBatch(allOrders, platformStoreId, merchantCode, erpStoreCode);

                submitPendingRetries(platformStoreId, merchantCode, erpStoreCode);

                Long finalSyncTime = (lastSuccessTimestamp != null && lastSuccessTimestamp > startTime)
                        ? lastSuccessTimestamp
                        : startTime;

                long successCount = allOrders.stream().filter(this::isTerminalOrder).count();
                int failCount = (int) (allOrders.size() - successCount);
                LocalDateTime syncEndTime = LocalDateTime.now();

                List<OrderListRespDTO.OrderDetail> kafkaOrders = allOrders.stream()
                        .filter(this::isTerminalOrder)
                        .collect(Collectors.toList());

                EleOrderSyncLog existingLog = eleOrderSyncLogMapper.selectByStoreId(platformStoreId);
                if (existingLog != null) {
                    existingLog.setMerchantCode(merchantCode);
                    existingLog.setErpStoreCode(erpStoreCode);
                    existingLog.setStoreName(storeName);
                    existingLog.setLastSyncTime(startTime);
                    existingLog.setSyncStartTime(syncStartTime);
                    existingLog.setSyncEndTime(syncEndTime);
                    existingLog.setSyncTime(finalSyncTime);
                    existingLog.setSyncCount(existingLog.getSyncCount() + allOrders.size());
                    existingLog.setSuccessCount(existingLog.getSuccessCount() + (int) successCount);
                    existingLog.setFailCount(existingLog.getFailCount() + failCount);
                    existingLog.setStatus(1);
                    existingLog.setErrorMsg(null);
                    eleOrderSyncLogMapper.updateById(existingLog);
                } else {
                    EleOrderSyncLog syncLog = new EleOrderSyncLog();
                    syncLog.setPlatformStoreId(platformStoreId);
                    syncLog.setMerchantCode(merchantCode);
                    syncLog.setErpStoreCode(erpStoreCode);
                    syncLog.setStoreName(storeName);
                    syncLog.setLastSyncTime(startTime);
                    syncLog.setSyncStartTime(syncStartTime);
                    syncLog.setSyncEndTime(syncEndTime);
                    syncLog.setSyncTime(finalSyncTime);
                    syncLog.setSyncCount(allOrders.size());
                    syncLog.setSuccessCount((int) successCount);
                    syncLog.setFailCount(failCount);
                    syncLog.setStatus(1);
                    syncLog.setCreateTime(System.currentTimeMillis());
                    eleOrderSyncLogMapper.insert(syncLog);
                }

                log.info("【syncTime推进】门店{}，startTime={}，endTime={}，lastSuccess={}，finalSyncTime={}",
                        platformStoreId, startTime, endTime, lastSuccessTimestamp, finalSyncTime);

                for (OrderListRespDTO.OrderDetail detail : kafkaOrders) {
                    try {
                        eleOrderKafkaProducer
                                .sendOrderMessage(buildMessage(detail, platformStoreId, merchantCode, erpStoreCode));
                    } catch (Exception e) {
                        saveFailRecord(detail.getOrderId(), detail.getChannelOrderId(), "SYNC", "KAFKA_SEND",
                                e.getMessage(),
                                detail, null, 0, null, platformStoreId, merchantCode, erpStoreCode);
                    }
                }
            } catch (Exception e) {
                LocalDateTime syncEndTime = LocalDateTime.now();
                EleOrderSyncLog existingLog = eleOrderSyncLogMapper.selectByStoreId(platformStoreId);
                if (existingLog != null) {
                    existingLog.setStatus(0);
                    existingLog.setSyncStartTime(syncStartTime);
                    existingLog.setSyncEndTime(syncEndTime);
                    if (existingLog.getSyncTime() == null) {
                        existingLog.setSyncTime(endTime != null ? endTime : System.currentTimeMillis() / 1000);
                    }
                    existingLog.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 1000
                            ? e.getMessage().substring(0, 1000)
                            : e.getMessage());
                    eleOrderSyncLogMapper.updateById(existingLog);
                } else {
                    EleOrderSyncLog failLog = new EleOrderSyncLog();
                    failLog.setPlatformStoreId(platformStoreId);
                    failLog.setMerchantCode(merchantCode);
                    failLog.setErpStoreCode(erpStoreCode);
                    failLog.setStoreName(storeName);
                    failLog.setLastSyncTime(startTime);
                    failLog.setSyncStartTime(syncStartTime);
                    failLog.setSyncEndTime(syncEndTime);
                    failLog.setSyncTime(endTime != null ? endTime : System.currentTimeMillis() / 1000);
                    failLog.setSyncCount(0);
                    failLog.setSuccessCount(0);
                    failLog.setFailCount(0);
                    failLog.setStatus(0);
                    failLog.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 1000
                            ? e.getMessage().substring(0, 1000)
                            : e.getMessage());
                    failLog.setCreateTime(System.currentTimeMillis());
                    eleOrderSyncLogMapper.insert(failLog);
                }
                log.error("【订单同步失败】门店{}，startTime={}，endTime={}，错误: {}", platformStoreId, startTime, endTime,
                        e.getMessage(), e);
                throw e;
            }
        } finally {
            shutdownStateManager.registerStoreSyncFinished(StrUtil.trim(store.getPlatformStoreId()));
        }
    }

    private void runSyncCycle(Long forcedStartTime, Long forcedEndTime) {
        if (!shutdownStateManager.startBatchSync()) {
            log.warn("【订单同步】已有批次在执行或应用正在关闭，跳过本次同步");
            return;
        }
        try {
            List<StorePlatformRespVO> stores = storeService.getOpenPlatformStoresByPlatformCode(null);
            if (stores == null || stores.isEmpty()) {
                log.info("暂无需要同步的门店");
                return;
            }

            syncTaskExecutor.executeSync(stores, forcedStartTime, forcedEndTime);
        } catch (Exception e) {
            saveFailRecord(null, null, "SYNC", "CYCLE", e.getMessage(), null, null, 0, null, null, null, null);
            log.error("门店同步任务执行异常: {}", e.getMessage(), e);
        } finally {
            shutdownStateManager.finishBatchSync();
        }
    }

    @Override
    public EleSyncSubmitRespDTO submitSyncTask(String platformStoreId, String merchantCode, String erpStoreCode,
            Long startTime, Long endTime, boolean compensate) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        EleCompensateProgressDTO progress = new EleCompensateProgressDTO();
        progress.setStatus("PROCESSING");
        progress.setPlatformStoreId(platformStoreId);
        progress.setMerchantCode(merchantCode);
        progress.setErpStoreCode(erpStoreCode);
        progress.setStartTime(startTime);
        progress.setEndTime(endTime);
        PROGRESS_CACHE.put(taskId, progress);

        compensateExecutor.execute(() -> {
            boolean locked = false;
            try {
                locked = eleOrderLockService.tryLockCompensate(taskId, 1, 10);
                if (!locked) {
                    log.warn("【补偿任务】获取锁失败, taskId={}", taskId);
                    progress.setStatus("FAILED");
                    progress.setErrorMessage("获取补偿任务锁失败");
                    return;
                }

                if (compensate) {
                    log.info("【补偿任务】开始执行, taskId={}, platformStoreId={}", taskId, platformStoreId);
                } else {
                    log.info("【同步任务】开始执行, taskId={}, platformStoreId={}", taskId, platformStoreId);
                }

                syncOrders(platformStoreId, merchantCode, erpStoreCode, startTime, endTime);
                progress.setStatus("SUCCESS");

                log.info("【{}】执行完成, taskId={}", compensate ? "补偿任务" : "同步任务", taskId);
            } catch (Exception e) {
                progress.setStatus("FAILED");
                progress.setErrorMessage(e.getMessage());
                saveFailRecord(null, null, compensate ? "COMPENSATE" : "SYNC", "TASK", e.getMessage(), null, null, 0,
                        taskId, platformStoreId, merchantCode, erpStoreCode);
                log.error("【{}】执行失败, taskId={}", compensate ? "补偿任务" : "同步任务", taskId, e);
            } finally {
                if (locked) {
                    eleOrderLockService.unlockCompensate(taskId);
                }
            }
        });

        EleSyncSubmitRespDTO resp = new EleSyncSubmitRespDTO();
        resp.setTaskId(taskId);
        resp.setStatus("PROCESSING");
        return resp;
    }

    @Override
    public EleCompensateProgressDTO getSyncProgress(String taskId) {
        return PROGRESS_CACHE.get(taskId);
    }

    @Override
    public List<EleOrderFailRecord> getFailRecords() {
        return eleOrderFailRecordMapper.selectList(new LambdaQueryWrapperX<EleOrderFailRecord>()
                .orderByDesc(EleOrderFailRecord::getCreateTime));
    }

    @Override
    public void retryFailRecord(Long id) {
        retryFailRecord(id, false);
    }

    @Override
    public void retryFailRecord(Long id, boolean overwrite) {
        EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("失败记录不存在");
        }
        if ("SUCCESS".equals(record.getProcessStatus())) {
            throw new RuntimeException("该失败记录已成功处理，无需重试");
        }

        boolean hasOrderId = record.getOrderId() != null && !record.getOrderId().isEmpty();
        boolean hasStoreInfo = record.getPlatformStoreId() != null && !record.getPlatformStoreId().isEmpty()
                && record.getMerchantCode() != null && !record.getMerchantCode().isEmpty()
                && record.getErpStoreCode() != null && !record.getErpStoreCode().isEmpty();

        if (hasOrderId && !hasStoreInfo) {
            EleOrder existingOrder = eleOrderMapper.selectByOrderId(record.getOrderId());
            if (existingOrder != null
                    && existingOrder.getPlatformStoreId() != null && !existingOrder.getPlatformStoreId().isEmpty()
                    && existingOrder.getMerchantCode() != null && !existingOrder.getMerchantCode().isEmpty()
                    && existingOrder.getErpStoreCode() != null && !existingOrder.getErpStoreCode().isEmpty()) {
                log.info("【重试补全门店信息】从ele_order表补全，orderId={}, platformStoreId={}", record.getOrderId(),
                        existingOrder.getPlatformStoreId());
                record.setPlatformStoreId(existingOrder.getPlatformStoreId());
                record.setMerchantCode(existingOrder.getMerchantCode());
                record.setErpStoreCode(existingOrder.getErpStoreCode());
                hasStoreInfo = true;
            }
        }

        if (hasOrderId && hasStoreInfo) {
            retryFailRecordForSingleOrder(record, overwrite);
        } else if (!hasOrderId && hasStoreInfo) {
            retryFailRecordForStoreSync(record);
        } else {
            record.setProcessStatus("PENDING_MANUAL");
            if (hasOrderId) {
                record.setRemark("无法自动重试：有orderId但缺少门店信息，且ele_order表中未找到对应记录，等待人工处理");
            } else {
                record.setRemark("无法自动重试：缺少orderId和门店信息，等待人工处理");
            }
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
            String detail = hasOrderId ? "有orderId但缺少门店信息" : "缺少orderId和门店信息";
            throw new RuntimeException("失败记录" + detail + "，无法自动重试，已标记为人工处理，记录ID=" + id);
        }
    }

    private void retryFailRecordForSingleOrder(EleOrderFailRecord record, boolean overwrite) {
        record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
        record.setProcessStatus("RETRYING");
        record.setUpdateTime(System.currentTimeMillis());
        eleOrderFailRecordMapper.updateById(record);

        try {
            OrderDetailRespDTO detail = getOrderDetailRemote(
                    record.getPlatformStoreId(), record.getMerchantCode(), record.getErpStoreCode(),
                    record.getOrderId());
            consumeOrderMessage(buildMessage(convertToOrderListDetail(detail), record.getPlatformStoreId(),
                    record.getMerchantCode(), record.getErpStoreCode()), overwrite);
            record.setProcessStatus("SUCCESS");
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
        } catch (Exception e) {
            record.setProcessStatus("FAILED");
            record.setFailMessage(e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000)
                    : e.getMessage());
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
            throw new RuntimeException("手动重试失败: " + e.getMessage(), e);
        }
    }

    private void retryFailRecordForStoreSync(EleOrderFailRecord record) {
        record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
        record.setProcessStatus("RETRYING");
        record.setUpdateTime(System.currentTimeMillis());
        eleOrderFailRecordMapper.updateById(record);

        try {
            syncOrders(record.getPlatformStoreId(), record.getMerchantCode(), record.getErpStoreCode());
            record.setProcessStatus("SUCCESS");
            record.setRemark("重新触发门店同步成功");
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
        } catch (Exception e) {
            record.setProcessStatus("FAILED");
            record.setFailMessage(e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000)
                    : e.getMessage());
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
            throw new RuntimeException("重新触发门店同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EleOrderStatusLog> getStatusLogs(String orderId) {
        return eleOrderStatusLogMapper.selectByOrderId(orderId);
    }

    @Override
    public PageResult<EleOrderFailRecordRespVO> getFailRecordPage(Long storeId, String orderId, String channelOrderId,
            String bizType, String failStage,
            String processStatus,
            Long startTime, Long endTime,
            Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);

        LambdaQueryWrapperX<EleOrderFailRecord> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(EleOrderFailRecord::getStoreId, storeId)
                .likeIfPresent(EleOrderFailRecord::getOrderId, orderId)
                .likeIfPresent(EleOrderFailRecord::getChannelOrderId, channelOrderId)
                .eqIfPresent(EleOrderFailRecord::getBizType, bizType)
                .eqIfPresent(EleOrderFailRecord::getFailStage, failStage)
                .eqIfPresent(EleOrderFailRecord::getProcessStatus, processStatus)
                .geIfPresent(EleOrderFailRecord::getCreateTime, startTime)
                .leIfPresent(EleOrderFailRecord::getCreateTime, endTime)
                .orderByDesc(EleOrderFailRecord::getCreateTime);

        return BeanUtils.toBean(eleOrderFailRecordMapper.selectPage(pageParam, wrapper),
                EleOrderFailRecordRespVO.class);
    }

    @Override
    public void batchRetryFailRecord(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            try {
                retryFailRecord(id);
            } catch (Exception e) {
                log.error("批量重试失败记录失败, id={}, error={}", id, e.getMessage());
            }
        }
    }

    @Override
    public int retryFailRecordsByTimeRange(Long startTime, Long endTime, Boolean overwrite) {
        List<EleOrderFailRecord> records = eleOrderFailRecordMapper.selectList(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .in(EleOrderFailRecord::getProcessStatus, "FAILED", "PENDING_RETRY")
                        .between(EleOrderFailRecord::getCreateTime, startTime, endTime)
                        .orderByAsc(EleOrderFailRecord::getCreateTime));

        if (records == null || records.isEmpty()) {
            log.info("【按时间重试】在时间范围[{}~{}]内没有需要重试的记录", startTime, endTime);
            return 0;
        }

        log.info("【按时间重试】找到{}条需要重试的记录，开始批量处理", records.size());

        int successCount = 0;
        boolean overwriteFlag = overwrite != null && overwrite;
        for (EleOrderFailRecord record : records) {
            try {
                retryFailRecord(record.getId(), overwriteFlag);
                successCount++;
            } catch (Exception e) {
                log.error("【按时间重试】重试记录失败, id={}, error={}", record.getId(), e.getMessage());
            }
        }

        log.info("【按时间重试】批量重试完成，共{}条记录，成功{}条", records.size(), successCount);
        return successCount;
    }

    private List<OrderListRespDTO.OrderDetail> pullAllOrders(String platformStoreId, String merchantCode,
            String erpStoreCode, Long startTime, Long endTime) {
        List<OrderListRespDTO.OrderDetail> allOrders = new ArrayList<>();
        final int pageSize = 100;

        // 只拉取状态为 -1（交易关闭）和 6（交易成功）的订单
        Integer[] targetStatuses = { -1, 6 };

        for (Integer status : targetStatuses) {
            String scrollId = null;

            while (true) {
                OrderListReqDTO req = new OrderListReqDTO();
                req.setPlatformStoreId(platformStoreId);
                req.setMerchantCode(merchantCode);
                req.setErpStoreCode(erpStoreCode);
                req.setStartTime(startTime);
                req.setEndTime(endTime);
                req.setStatus(status);
                req.setPageSize(pageSize);
                req.setScrollId(scrollId);

                ORDER_LIST_RATE_LIMITER.acquire();
                OrderListRespDTO pageResult = getOrderList(req);
                if (pageResult == null || pageResult.getOrderList() == null) {
                    break;
                }

                allOrders.addAll(pageResult.getOrderList());

                scrollId = pageResult.getScrollId();

                if (scrollId == null || scrollId.isEmpty()) {
                    break;
                }
            }
        }

        int originalCount = allOrders.size();
        allOrders = allOrders.stream()
                .filter(this::isTerminalOrder)
                .collect(Collectors.toList());

        log.info("【拉取订单】platformStoreId={}, 原始{}条，终态保留{}条", platformStoreId, originalCount, allOrders.size());
        for (OrderListRespDTO.OrderDetail order : allOrders) {
            log.info("【拉取订单】订单详情: orderId={}, channelOrderId={}, status={}, deliveryStatus={}, totalFee={}, payFee={}",
                    order.getOrderId(), order.getChannelOrderId(), order.getStatus(), order.getDeliveryStatus(),
                    order.getTotalFee(), order.getPayFee());
        }

        return allOrders;
    }

    @Override
    public PageResult<OrderListRespDTO.OrderDetail> getOrdersFromLocal(String platformStoreId, Integer status,
            Long startTime, Long endTime, Integer pageNo, Integer pageSize) {
        String storeCode = platformStoreId;

        if (startTime != null && startTime < 100000000000L) {
            startTime = startTime * 1000;
        }
        if (endTime != null && endTime < 100000000000L) {
            endTime = endTime * 1000;
        }

        List<OrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(storeCode != null, OrderDO::getStoreCode, storeCode)
                .eq(status != null, OrderDO::getOrderStatus, status)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false)
                .orderByDesc(OrderDO::getCreateTime)
                .last("LIMIT " + ((pageNo - 1) * pageSize) + ", " + pageSize));

        Long total = orderMapper.selectCount(new LambdaQueryWrapperX<OrderDO>()
                .eq(storeCode != null, OrderDO::getStoreCode, storeCode)
                .eq(status != null, OrderDO::getOrderStatus, status)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false));

        return new PageResult<>(eleOrderConvertService.assembleOrderList(orders), total);
    }

    @Override
    public OrderDetailRespDTO getDetailFromLocal(String orderId) {
        List<OrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getOrderId, orderId)
                .eq(OrderDO::getDeleted, false)
                .last("LIMIT 1"));
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        return eleOrderConvertService.assembleOrderDetail(orders.get(0));
    }

    @Override
    public Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode) {
        return saveOrUpdateBatch(orders, platformStoreId, merchantCode, erpStoreCode, false);
    }

    @Override
    public Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, boolean overwrite) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }

        // 1. 过滤出终态订单
        List<OrderListRespDTO.OrderDetail> terminalOrders = orders.stream()
                .filter(this::isTerminalOrder)
                .collect(Collectors.toList());

        if (terminalOrders.isEmpty()) {
            return null;
        }

        Long lastSuccessTimestamp = null;

        // 2. 批量查询已存在的订单（1次SQL代替N次查询）
        List<String> orderIds = terminalOrders.stream()
                .map(OrderListRespDTO.OrderDetail::getOrderId)
                .collect(Collectors.toList());

        List<String> existingIds = orderMapper.selectExistingOrderIds(orderIds);
        Set<String> existingIdSet = new HashSet<>(existingIds);

        // 3. 分离新订单和已存在订单
        List<OrderListRespDTO.OrderDetail> newOrders = new ArrayList<>();
        List<OrderListRespDTO.OrderDetail> existOrders = new ArrayList<>();

        for (OrderListRespDTO.OrderDetail order : terminalOrders) {
            if (existingIdSet.contains(order.getOrderId())) {
                existOrders.add(order);
            } else {
                newOrders.add(order);
            }
        }

        log.info("【订单分类】总计{}个终态订单，新订单{}个，已存在{}个",
                terminalOrders.size(), newOrders.size(), existOrders.size());

        // 4. 批量插入新订单（无锁，高性能）
        if (!newOrders.isEmpty()) {
            try {
                batchInsertNewOrders(newOrders, erpStoreCode);

                // 更新 lastSuccessTimestamp
                for (OrderListRespDTO.OrderDetail order : newOrders) {
                    if (order.getCreateTime() != null) {
                        if (lastSuccessTimestamp == null || order.getCreateTime() > lastSuccessTimestamp) {
                            lastSuccessTimestamp = order.getCreateTime();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("【批量插入失败】降级为逐个插入，错误: {}", e.getMessage(), e);
                // 降级：批量失败时逐个插入
                for (OrderListRespDTO.OrderDetail order : newOrders) {
                    processSingleOrder(order, platformStoreId, merchantCode, erpStoreCode, overwrite);
                    if (order.getCreateTime() != null) {
                        if (lastSuccessTimestamp == null || order.getCreateTime() > lastSuccessTimestamp) {
                            lastSuccessTimestamp = order.getCreateTime();
                        }
                    }
                }
            }
        }

        // 5. 逐个处理已存在订单（保持原有锁逻辑）
        for (OrderListRespDTO.OrderDetail order : existOrders) {
            processSingleOrder(order, platformStoreId, merchantCode, erpStoreCode, overwrite);
            if (order.getCreateTime() != null) {
                if (lastSuccessTimestamp == null || order.getCreateTime() > lastSuccessTimestamp) {
                    lastSuccessTimestamp = order.getCreateTime();
                }
            }
        }

        return lastSuccessTimestamp;
    }

    /**
     * 批量插入新订单（无锁，适合新订单场景）
     * 
     * @param newOrders    新订单列表
     * @param erpStoreCode ERP门店编码
     */
    private void batchInsertNewOrders(List<OrderListRespDTO.OrderDetail> newOrders, String erpStoreCode) {
        if (newOrders.isEmpty()) {
            return;
        }

        log.info("【批量插入】开始批量插入{}个新订单", newOrders.size());

        // 按 50 个为一组批量插入
        int batchSize = 50;
        for (int i = 0; i < newOrders.size(); i += batchSize) {
            List<OrderListRespDTO.OrderDetail> batch = newOrders.subList(
                    i, Math.min(i + batchSize, newOrders.size()));

            // 批量插入订单主表
            for (OrderListRespDTO.OrderDetail order : batch) {
                OrderDO orderDO = buildOrderDO(order, erpStoreCode);
                orderMapper.insert(orderDO);
            }

            // 批量插入平台表
            for (OrderListRespDTO.OrderDetail order : batch) {
                upsertPlatform(order);
            }

            // 批量插入商品明细表
            for (OrderListRespDTO.OrderDetail order : batch) {
                replaceItems(order, erpStoreCode);
            }

            // 批量插入优惠表
            for (OrderListRespDTO.OrderDetail order : batch) {
                replaceDiscounts(order);
            }

            log.info("【批量插入】已完成 {}/{} 个订单", Math.min(i + batchSize, newOrders.size()), newOrders.size());
        }

        log.info("【批量插入】完成{}个新订单插入", newOrders.size());
    }

    /**
     * 处理单个订单（抽取原有逻辑，用于降级和已存在订单处理）
     */
    private void processSingleOrder(OrderListRespDTO.OrderDetail orderDetail,
            String platformStoreId, String merchantCode, String erpStoreCode, boolean overwrite) {

        final String orderId = orderDetail.getOrderId();
        try {
            RetryUtil.executeWithRetry(() -> {
                saveSingleOrder(orderDetail, erpStoreCode, overwrite);
                return null;
            }, "订单落库-" + orderId, 1, 100, 500);
        } catch (Exception e) {
            log.error("【订单落库失败】orderId={}，快速失败入队，错误: {}", orderId, e.getMessage());
            EleOrderFailRecord record = saveFailRecordReturn(orderId, orderDetail.getChannelOrderId(),
                    "SYNC", "DB_INSERT", e.getMessage(), orderDetail, null, 0, null, "PENDING_RETRY",
                    platformStoreId, merchantCode, erpStoreCode);

            EleOrderRetryTaskSubmitter.RetryTask retryTask = new EleOrderRetryTaskSubmitter.RetryTask(
                    orderId, orderDetail.getChannelOrderId(),
                    platformStoreId, merchantCode, erpStoreCode,
                    record.getId(), orderDetail);
            List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
            tasks.add(retryTask);
            retryTaskSubmitter.submitRetryTasks(tasks);
        }
    }

    private void saveSingleOrder(OrderListRespDTO.OrderDetail orderDetail, String erpStoreCode, boolean overwrite) {
        String orderId = orderDetail.getOrderId();

        if (shutdownStateManager.isShuttingDown()) {
            log.warn("【订单同步】应用正在关闭，跳过订单处理，orderId={}", orderId);
            throw new EleOrderSyncException("【订单同步】应用正在关闭，跳过订单处理，orderId=" + orderId);
        }

        shutdownStateManager.taskStarted(orderId);
        LockResult lockResult = tryAcquireLock(orderId);

        try {
            doSaveSingleOrder(orderDetail, erpStoreCode, overwrite);
        } finally {
            if (lockResult.isLocked()) {
                eleOrderLockService.unlockOrder(orderId);
            }
            shutdownStateManager.taskFinished();
        }
    }

    private void saveSingleOrder(OrderListRespDTO.OrderDetail orderDetail, String erpStoreCode) {
        saveSingleOrder(orderDetail, erpStoreCode, false);
    }

    private LockResult tryAcquireLock(String orderId) {
        try {
            boolean locked = eleOrderLockService.tryLockOrderWithWatchdog(orderId, 30);
            if (!locked) {
                throw new EleOrderSyncException("【订单锁】获取锁失败，订单正在被其他线程处理，orderId=" + orderId);
            }
            return new LockResult(true);
        } catch (RedissonShutdownException e) {
            log.warn("【订单锁】应用关闭中，Redisson 已停止，跳过订单同步，orderId={}", orderId);
            throw new EleOrderSyncException("【订单锁】应用正在关闭，Redisson 已停止，orderId=" + orderId, e);
        } catch (EleOrderSyncException e) {
            throw e;
        } catch (Exception e) {
            log.error("【订单锁】Redis异常，orderId={}", orderId, e);
            throw new EleOrderSyncException("【订单锁】Redis连接异常，无法获取分布式锁，orderId=" + orderId, e);
        }
    }

    private static class LockResult {
        private final boolean locked;

        public LockResult(boolean locked) {
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked;
        }
    }

    private void doSaveSingleOrder(OrderListRespDTO.OrderDetail orderDetail, String erpStoreCode, boolean overwrite) {
        List<OrderDO> existingList = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getOrderId, orderDetail.getOrderId())
                .eq(OrderDO::getDeleted, false)
                .last("LIMIT 1"));

        if (existingList != null && !existingList.isEmpty()) {
            if (overwrite) {
                log.info("【覆盖订单】订单已存在，执行覆盖更新，orderId={}", orderDetail.getOrderId());
                doUpdateOrder(existingList.get(0), orderDetail, erpStoreCode);
            } else {
                log.info("【跳过更新】订单已存在，不修改任何字段，orderId={}", orderDetail.getOrderId());
                return;
            }
        } else {
            OrderDO order = buildOrderDO(orderDetail, erpStoreCode);
            orderMapper.insert(order);
            // 已禁用订单状态日志写入：落库状态即为最终状态，无需记录变更日志
            // Long storeId = parseStoreId(erpStoreCode);
            // insertStatusLog(storeId, null, null, orderDetail, "SYNC", "首次落库");
        }

        upsertPlatform(orderDetail);
        replaceItems(orderDetail, erpStoreCode);
        replaceDiscounts(orderDetail);
    }

    private void doSaveSingleOrder(OrderListRespDTO.OrderDetail orderDetail, String erpStoreCode) {
        doSaveSingleOrder(orderDetail, erpStoreCode, false);
    }

    private void doUpdateOrder(OrderDO existingOrder, OrderListRespDTO.OrderDetail orderDetail, String erpStoreCode) {
        OrderDO order = buildOrderDO(orderDetail, erpStoreCode);
        // 使用包装器根据 orderId 进行更新
        orderMapper.update(order, new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getOrderId, existingOrder.getOrderId()));

        // 已禁用订单状态日志写入：落库状态即为最终状态，无需记录变更日志
        // Long storeId = parseStoreId(erpStoreCode);
        // insertStatusLog(storeId, null, null, orderDetail, "SYNC", "覆盖更新");

        upsertPlatform(orderDetail);
        replaceItems(orderDetail, erpStoreCode);
        replaceDiscounts(orderDetail);
    }

    @Override
    public void consumeOrderMessage(OrderMessage message) {
        consumeOrderMessage(message, false);
    }

    @Override
    public void consumeOrderMessage(OrderMessage message, boolean overwrite) {
        saveOrUpdateBatch(List.of(convertMessageToOrderDetail(message)), message.getPlatformStoreId(),
                message.getMerchantCode(), message.getErpStoreCode(), overwrite);
    }

    private OrderDO buildOrderDO(OrderListRespDTO.OrderDetail detail, String erpStoreCode) {
        OrderDO order = new OrderDO();
        order.setOrderId(detail.getOrderId());
        order.setOrderStatus(detail.getStatus());
        order.setCreateTime(detail.getCreateTime());
        order.setPayTime(detail.getPayTime());
        order.setBuyerName(detail.getBuyerName());
        order.setBuyerPhone(detail.getBuyerPhone());
        order.setBuyerAddress(detail.getBuyerAddress());
        order.setDeliveryName(detail.getDeliveryName());
        order.setDeliveryPhone(detail.getDeliveryPhone());
        order.setDeliveryStatus(detail.getDeliveryStatus());
        order.setTotalFee(fenToYuan(detail.getTotalFee()));
        order.setPayFee(fenToYuan(detail.getPayFee()));
        order.setDiscountFee(fenToYuan(detail.getDiscountFee()));
        order.setDeliveryFee(fenToYuan(detail.getDeliveryFee()));
        order.setPostFee(fenToYuan(detail.getPostFee()));
        order.setPackageFee(fenToYuan(detail.getPackageFee()));
        order.setChannelSourceId(detail.getChannelSourceId());
        order.setChannelSourceName(detail.getChannelSourceName());
        order.setChannelOrderId(detail.getChannelOrderId());
        order.setStoreCode(resolveStoreCode(detail, erpStoreCode));
        order.setLongitude(detail.getLongitude());
        order.setLatitude(detail.getLatitude());
        order.setUserId(getUserId(detail.getUserId()));
        order.setRemark(detail.getRemark());
        order.setArriveType(detail.getArriveType());
        order.setCreator("admin_sync");
        order.setUpdateTime(System.currentTimeMillis());
        order.setDeleted(false);
        return order;
    }

    private void upsertPlatform(OrderListRespDTO.OrderDetail detail) {
        OrderPlatformDO platform = new OrderPlatformDO();
        platform.setOrderId(detail.getOrderId());
        platform.setPlatformType(detail.getChannelType());
        platform.setDeliveryPlatform(
                detail.getDeliveryPlatform() == null ? null : Integer.valueOf(detail.getDeliveryPlatform()));
        platform.setDeliveryType(detail.getDeliveryType());
        platform.setPlatformCommissionFee(fenToYuan(detail.getPlatformCommissionFee()));
        platform.setPlatformOrderStatus(detail.getStatus() == null ? null : String.valueOf(detail.getStatus()));
        platform.setPlatformDeliveryStatus(
                detail.getDeliveryStatus() == null ? null : String.valueOf(detail.getDeliveryStatus()));
        platform.setUpdateTime(System.currentTimeMillis());
        platform.setDeleted(false);

        List<OrderPlatformDO> existingList = orderPlatformMapper.selectList(new LambdaQueryWrapperX<OrderPlatformDO>()
                .eq(OrderPlatformDO::getOrderId, detail.getOrderId())
                .eq(OrderPlatformDO::getDeleted, false)
                .last("LIMIT 1"));
        if (existingList == null || existingList.isEmpty()) {
            platform.setCreateTime(System.currentTimeMillis());
            orderPlatformMapper.insert(platform);
        } else {
            orderPlatformMapper.update(platform, new LambdaQueryWrapperX<OrderPlatformDO>()
                    .eq(OrderPlatformDO::getOrderId, detail.getOrderId()));
        }
    }

    private void saveRemoteOrderDetailToLocal(OrderDetailRespDTO detail, String erpStoreCode) {
        if (detail == null || detail.getOrderId() == null) {
            return;
        }
        if (!isTerminalStatus(detail.getStatus())) {
            log.info("【跳过详情落库】非目标终态订单，orderId={}, status={}", detail.getOrderId(), detail.getStatus());
            return;
        }

        OrderListRespDTO.OrderDetail orderDetail = convertToOrderListDetail(detail);
        saveSingleOrder(orderDetail, erpStoreCode);
    }

    private OrderListRespDTO.OrderDetail convertToOrderListDetail(OrderDetailRespDTO dto) {
        OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
        detail.setOrderId(dto.getOrderId());
        detail.setStatus(dto.getStatus());
        detail.setCreateTime(dto.getCreateTime());
        detail.setPayTime(dto.getPayTime());
        detail.setChannelSourceName(dto.getChannelSourceName());
        detail.setBuyerName(dto.getBuyerName());
        detail.setBuyerPhone(dto.getBuyerPhone());
        detail.setBuyerAddress(dto.getBuyerAddress());
        detail.setDeliveryName(dto.getDeliveryName());
        detail.setDeliveryPhone(dto.getDeliveryPhone());
        detail.setDeliveryPlatform(dto.getDeliveryPlatform());
        detail.setDeliveryType(dto.getDeliveryType());
        detail.setDeliveryStatus(dto.getDeliveryStatus());
        detail.setTotalFee(dto.getTotalFee());
        detail.setPayFee(dto.getPayFee());
        detail.setDiscountFee(dto.getDiscountFee());
        detail.setDeliveryFee(dto.getDeliveryFee());
        detail.setPostFee(dto.getPostFee());
        detail.setPackageFee(dto.getPackageFee());
        detail.setPlatformCommissionFee(dto.getPlatformCommissionFee());
        detail.setRemark(dto.getRemark());
        detail.setChannelSourceId(dto.getChannelSourceId());
        detail.setChannelOrderId(dto.getChannelOrderId());
        detail.setChannelType(dto.getChannelType());
        detail.setStoreCode(dto.getStoreCode());
        detail.setErpStoreCode(dto.getErpStoreCode());
        detail.setLongitude(dto.getLongitude());
        detail.setLatitude(dto.getLatitude());
        detail.setSubOrders(dto.getSubOrders());
        detail.setDiscounts(dto.getDiscounts());
        return detail;
    }

    private void updatePlatformDeliveryStatus(String orderId, Integer deliveryStatus) {
        if (orderId == null || deliveryStatus == null) {
            return;
        }
        List<OrderPlatformDO> platformList = orderPlatformMapper.selectList(new LambdaQueryWrapperX<OrderPlatformDO>()
                .eq(OrderPlatformDO::getOrderId, orderId)
                .eq(OrderPlatformDO::getDeleted, false)
                .last("LIMIT 1"));
        OrderPlatformDO platform = platformList != null && !platformList.isEmpty() ? platformList.get(0) : null;
        if (platform != null) {
            platform.setPlatformDeliveryStatus(EleDeliveryStatusEnum.getNameByStatus(deliveryStatus));
            platform.setUpdateTime(System.currentTimeMillis());
            orderPlatformMapper.update(platform, new LambdaQueryWrapperX<OrderPlatformDO>()
                    .eq(OrderPlatformDO::getOrderId, orderId));
        }
    }

    private void replaceItems(OrderListRespDTO.OrderDetail detail, String erpStoreCode) {
        orderItemMapper.delete(new LambdaQueryWrapperX<OrderItemDO>()
                .eq(OrderItemDO::getOrderId, detail.getOrderId()));
        if (detail.getSubOrders() == null || detail.getSubOrders().isEmpty()) {
            return;
        }
        for (int i = 0; i < detail.getSubOrders().size(); i++) {
            OrderDetailRespDTO.SubOrder subOrder = detail.getSubOrders().get(i);
            OrderItemDO item = new OrderItemDO();
            item.setSubOrderId(
                    subOrder.getSubOrderId() != null ? subOrder.getSubOrderId() : detail.getOrderId() + "-" + i);
            item.setOrderId(detail.getOrderId());
            item.setErpStoreCode(erpStoreCode);
            item.setSkuCode(subOrder.getSkuCode());
            item.setSkuName(subOrder.getSkuName());
            item.setBarcode(subOrder.getBarcode());
            item.setSpecification(subOrder.getSpecification());
            item.setWeight(subOrder.getWeight() == null ? null : new BigDecimal(subOrder.getWeight()));
            item.setTotalWeight(calculateTotalWeight(subOrder.getWeight(), subOrder.getBuyAmount()));
            item.setBuyAmount(subOrder.getBuyAmount());
            item.setNum(subOrder.getNum());
            item.setPrice(fenToYuan(subOrder.getPrice()));
            item.setTotalFee(fenToYuan(subOrder.getTotalFee()));
            item.setPayFee(fenToYuan(subOrder.getPayFee()));
            item.setProductType(parseProductType(subOrder.getGoodsType()));
            item.setCreator("admin_sync");
            item.setCreateTime(System.currentTimeMillis());
            item.setUpdater("admin_sync");
            item.setUpdateTime(System.currentTimeMillis());
            item.setDeleted(false);
            orderItemMapper.insert(item);
        }
    }

    private Integer parseProductType(String goodsType) {
        if (goodsType == null) {
            return 0;
        }
        return switch (goodsType) {
            case "0" -> 0;
            case "3" -> 1;
            case "2" -> 2;
            default -> 0;
        };
    }

    private void replaceDiscounts(OrderListRespDTO.OrderDetail detail) {
        orderDiscountMapper.delete(new LambdaQueryWrapperX<OrderDiscountDO>()
                .eq(OrderDiscountDO::getOrderId, detail.getOrderId()));
        if (detail.getDiscounts() == null || detail.getDiscounts().isEmpty()) {
            return;
        }
        for (int i = 0; i < detail.getDiscounts().size(); i++) {
            OrderDetailRespDTO.Discount discount = detail.getDiscounts().get(i);
            OrderDiscountDO discountDO = new OrderDiscountDO();
            discountDO.setOrderId(detail.getOrderId());
            discountDO.setActivityId(discount.getActivityId());
            discountDO.setActivityName(discount.getActivityName());
            discountDO.setActivityOrderType(discount.getActivityOrderType() != null
                    ? String.valueOf(discount.getActivityOrderType())
                    : null);
            discountDO.setDiscountType(discount.getType());
            discountDO.setDiscountFee(fenToYuan(discount.getDiscountFee()));
            discountDO.setMerchantFee(fenToYuan(discount.getMerchantFee()));
            discountDO.setPlatformFee(fenToYuan(discount.getPlatformFee()));
            discountDO.setCreator("admin_sync");
            discountDO.setCreateTime(System.currentTimeMillis());
            discountDO.setUpdater("admin_sync");
            discountDO.setUpdateTime(System.currentTimeMillis());
            discountDO.setDeleted(false);
            orderDiscountMapper.insert(discountDO);
        }
    }

    private Long normalizeEleOrderEpochSecondsForQuery(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp > 100000000000L ? timestamp / 1000 : timestamp;
    }

    private OrderMessage buildMessage(OrderListRespDTO.OrderDetail detail, String platformStoreId, String merchantCode,
            String erpStoreCode) {
        OrderMessage message = new OrderMessage();
        String resolvedMerchantCode = StrUtil.blankToDefault(merchantCode,
                StrUtil.trim(getApiConfig().getMerchantCode()));
        message.setOrderId(detail.getOrderId());
        message.setPlatformStoreId(platformStoreId);
        message.setMerchantCode(resolvedMerchantCode);
        message.setErpStoreCode(erpStoreCode != null ? erpStoreCode : detail.getErpStoreCode());
        message.setStatus(detail.getStatus());
        message.setCreateTime(detail.getCreateTime());
        message.setPayTime(detail.getPayTime());
        message.setChannelSourceName(detail.getChannelSourceName());
        message.setBuyerName(detail.getBuyerName());
        message.setBuyerPhone(detail.getBuyerPhone());
        message.setBuyerAddress(detail.getBuyerAddress());
        message.setDeliveryName(detail.getDeliveryName());
        message.setDeliveryPhone(detail.getDeliveryPhone());
        message.setDeliveryPlatform(detail.getDeliveryPlatform());
        message.setDeliveryType(detail.getDeliveryType());
        message.setDeliveryStatus(detail.getDeliveryStatus());
        message.setTotalFee(detail.getTotalFee());
        message.setPayFee(detail.getPayFee());
        message.setDiscountFee(detail.getDiscountFee());
        message.setDeliveryFee(detail.getDeliveryFee());
        message.setPostFee(detail.getPostFee());
        message.setPackageFee(detail.getPackageFee());
        message.setPlatformCommissionFee(detail.getPlatformCommissionFee());
        message.setRemark(detail.getRemark());
        message.setChannelSourceId(detail.getChannelSourceId());
        message.setChannelOrderId(detail.getChannelOrderId());
        message.setChannelType(detail.getChannelType());
        message.setStoreCode(detail.getStoreCode());
        message.setLongitude(detail.getLongitude());
        message.setLatitude(detail.getLatitude());
        message.setArriveType(detail.getArriveType());
        try {
            message.setSubOrdersJson(
                    detail.getSubOrders() == null ? null : objectMapper.writeValueAsString(detail.getSubOrders()));
            message.setDiscountsJson(
                    detail.getDiscounts() == null ? null : objectMapper.writeValueAsString(detail.getDiscounts()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        message.setRealtime(Boolean.TRUE);
        message.setMessageTime(System.currentTimeMillis());
        message.setRetryCount(0);
        return message;
    }

    private OrderListRespDTO.OrderDetail convertMessageToOrderDetail(OrderMessage message) {
        OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
        detail.setOrderId(message.getOrderId());
        detail.setStatus(message.getStatus());
        detail.setCreateTime(message.getCreateTime());
        detail.setPayTime(message.getPayTime());
        detail.setChannelSourceName(message.getChannelSourceName());
        detail.setBuyerName(message.getBuyerName());
        detail.setBuyerPhone(message.getBuyerPhone());
        detail.setBuyerAddress(message.getBuyerAddress());
        detail.setDeliveryName(message.getDeliveryName());
        detail.setDeliveryPhone(message.getDeliveryPhone());
        detail.setDeliveryPlatform(message.getDeliveryPlatform());
        detail.setDeliveryType(message.getDeliveryType());
        detail.setDeliveryStatus(message.getDeliveryStatus());
        detail.setTotalFee(message.getTotalFee());
        detail.setPayFee(message.getPayFee());
        detail.setDiscountFee(message.getDiscountFee());
        detail.setDeliveryFee(message.getDeliveryFee());
        detail.setPostFee(message.getPostFee());
        detail.setPackageFee(message.getPackageFee());
        detail.setPlatformCommissionFee(message.getPlatformCommissionFee());
        detail.setRemark(message.getRemark());
        detail.setChannelSourceId(message.getChannelSourceId());
        detail.setChannelOrderId(message.getChannelOrderId());
        detail.setChannelType(message.getChannelType());
        detail.setStoreCode(message.getStoreCode());
        detail.setErpStoreCode(message.getErpStoreCode());
        detail.setLongitude(message.getLongitude());
        detail.setLatitude(message.getLatitude());
        detail.setArriveType(message.getArriveType());
        try {
            if (message.getSubOrdersJson() != null) {
                detail.setSubOrders(objectMapper.readValue(message.getSubOrdersJson(),
                        new TypeReference<List<OrderDetailRespDTO.SubOrder>>() {
                        }));
            }
            if (message.getDiscountsJson() != null) {
                detail.setDiscounts(objectMapper.readValue(message.getDiscountsJson(),
                        new TypeReference<List<OrderDetailRespDTO.Discount>>() {
                        }));
            }
        } catch (

        JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return detail;
    }

    private void insertStatusLog(Long storeId, OrderDO existing, OrderPlatformDO existingPlatform,
            OrderListRespDTO.OrderDetail detail, String source, String reason) {
        EleOrderStatusLog logRecord = new EleOrderStatusLog();
        logRecord.setPlatformType(PLATFORM);
        logRecord.setOrderId(detail.getOrderId());
        logRecord.setChannelOrderId(detail.getChannelOrderId());
        logRecord.setStoreId(storeId);
        logRecord.setBeforeOrderStatus(existing == null || existing.getOrderStatus() == null ? null
                : String.valueOf(existing.getOrderStatus()));
        logRecord.setAfterOrderStatus(detail.getStatus() == null ? null : String.valueOf(detail.getStatus()));
        logRecord.setBeforeDeliveryStatus(existing == null || existing.getDeliveryStatus() == null ? null
                : String.valueOf(existing.getDeliveryStatus()));
        logRecord.setAfterDeliveryStatus(
                detail.getDeliveryStatus() == null ? null : String.valueOf(detail.getDeliveryStatus()));
        logRecord.setBeforePlatformStatus(existingPlatform == null ? null : existingPlatform.getPlatformOrderStatus());
        logRecord.setAfterPlatformStatus(detail.getStatus() == null ? null : String.valueOf(detail.getStatus()));
        logRecord.setChangeSource(source);
        logRecord.setChangeReason(reason);
        try {
            logRecord.setSnapshotContent(objectMapper.writeValueAsString(detail));
        } catch (JsonProcessingException e) {
            logRecord.setSnapshotContent(null);
        }
        logRecord.setCreateTime(System.currentTimeMillis());
        eleOrderStatusLogMapper.insert(logRecord);
    }

    private void saveFailRecord(String orderId, String channelOrderId, String bizType, String failStage,
            String message, Object request, Object response, Integer retryCount, String taskId,
            String platformStoreId, String merchantCode, String erpStoreCode) {
        saveFailRecordReturn(orderId, channelOrderId, bizType, failStage, message, request, response, retryCount,
                taskId, "FAILED", platformStoreId, merchantCode, erpStoreCode);
    }

    private EleOrderFailRecord saveFailRecordReturn(String orderId, String channelOrderId, String bizType,
            String failStage, String message, Object request, Object response, Integer retryCount, String taskId,
            String processStatus, String platformStoreId, String merchantCode, String erpStoreCode) {
        EleOrderFailRecord record = new EleOrderFailRecord();
        record.setPlatformType(PLATFORM);
        record.setOrderId(orderId);
        record.setChannelOrderId(channelOrderId);
        record.setBizType(bizType);
        record.setFailStage(failStage);
        record.setFailMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
        try {
            record.setRequestParam(request == null ? null : objectMapper.writeValueAsString(request));
            record.setResponseContent(response == null ? null : objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            record.setRequestParam(null);
            record.setResponseContent(null);
        }
        record.setRetryCount(retryCount == null ? 0 : retryCount);
        record.setMaxRetryCount(3);
        record.setProcessStatus(processStatus != null ? processStatus : "FAILED");
        record.setTaskId(taskId);
        record.setPlatformStoreId(platformStoreId);
        record.setMerchantCode(merchantCode);
        record.setErpStoreCode(erpStoreCode);
        String traceId = TracerUtils.getTraceId();
        if (StrUtil.isNotBlank(traceId)) {
            record.setRemark("traceId: " + traceId);
        }
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        eleOrderFailRecordMapper.insert(record);
        return record;
    }

    private void submitPendingRetries(String platformStoreId, String merchantCode, String erpStoreCode) {
        List<EleOrderFailRecord> pendingRecords = eleOrderFailRecordMapper.selectList(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "PENDING_RETRY")
                        .eq(EleOrderFailRecord::getPlatformStoreId, platformStoreId)
                        .lt(EleOrderFailRecord::getRetryCount, 3));

        if (pendingRecords.isEmpty()) {
            log.debug("【重试Kafka】门店{}无等待重试的订单", platformStoreId);
            return;
        }

        log.info("【重试Kafka】门店{}同步完成，提交{}个订单到Kafka进行异步重试", platformStoreId, pendingRecords.size());

        List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
        for (EleOrderFailRecord record : pendingRecords) {
            tasks.add(new EleOrderRetryTaskSubmitter.RetryTask(
                    record.getOrderId(), record.getChannelOrderId(),
                    platformStoreId, merchantCode, erpStoreCode,
                    record.getId(), null));
        }
        retryTaskSubmitter.submitRetryTasks(tasks);
    }

    private void clearFailRecordIfSuccess(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return;
        }
        try {
            List<EleOrderFailRecord> failRecords = eleOrderFailRecordMapper.selectList(
                    new LambdaQueryWrapperX<EleOrderFailRecord>()
                            .eq(EleOrderFailRecord::getOrderId, orderId)
                            .in(EleOrderFailRecord::getProcessStatus, "FAILED", "RETRYING", "PENDING_RETRY")
                            .orderByDesc(EleOrderFailRecord::getCreateTime));

            if (failRecords == null || failRecords.isEmpty()) {
                return;
            }

            for (EleOrderFailRecord record : failRecords) {
                record.setProcessStatus("SUCCESS");
                record.setUpdateTime(System.currentTimeMillis());
                if (record.getRemark() == null || record.getRemark().isEmpty()) {
                    record.setRemark("订单已成功落库，自动清理");
                } else {
                    record.setRemark(record.getRemark() + " | 订单已成功落库，自动清理");
                }
                eleOrderFailRecordMapper.updateById(record);
                log.info("【清理失败记录】orderId={}, recordId={}, processStatus=SUCCESS", orderId, record.getId());
            }
        } catch (Exception e) {
            log.warn("【清理失败记录异常】orderId={}, error={}", orderId, e.getMessage());
        }
    }

    @Override
    public List<OrderListRespDTO.OrderDetail> getAllStoreOrdersWithDetails() {
        log.info("【查询所有门店订单】开始查询所有门店订单...");

        List<OrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getDeleted, false)
                .orderByDesc(OrderDO::getCreateTime)
                .last("LIMIT 1000"));

        if (CollUtil.isEmpty(orders)) {
            log.info("【查询所有门店订单】数据库中暂无订单");
            return new ArrayList<>();
        }

        log.info("【查询所有门店订单】查询到{}条订单，开始组装订单详情", orders.size());

        List<OrderListRespDTO.OrderDetail> result = eleOrderConvertService.assembleOrderList(orders);

        log.info("【查询所有门店订单】组装完成，共{}条订单详情", result.size());
        return result;
    }

    @Override
    public void syncAllStores() {
        syncAllStores(null, null);
    }

    @Override
    public void syncAllStores(Long startTime, Long endTime) {
        if (startTime != null && startTime > 100000000000L) {
            startTime = startTime / 1000;
        }
        if (endTime != null && endTime > 100000000000L) {
            endTime = endTime / 1000;
        }
        log.info("【手动全量同步】开始手动触发全部门店订单同步，startTime={}, endTime={}", startTime, endTime);
        runSyncCycle(startTime, endTime);
        log.info("【手动全量同步】全部门店订单同步任务已提交完成");
    }

    @Override
    public java.util.Map<String, Object> syncAllStoresWithResult() {
        log.info("【手动全量同步-详细】开始手动触发全部门店订单同步...");
        if (!shutdownStateManager.startBatchSync()) {
            log.warn("【手动全量同步-详细】已有批次在执行或应用正在关闭，跳过本次同步");
            return java.util.Map.of(
                    "message", "已有批次在执行或应用正在关闭，跳过本次同步",
                    "totalCount", 0,
                    "successCount", 0,
                    "failCount", 0,
                    "completed", false);
        }
        try {
            List<StorePlatformRespVO> stores = storeService.getOpenPlatformStoresByPlatformCode(null);
            if (stores == null || stores.isEmpty()) {
                log.info("【手动全量同步-详细】暂无需要同步的门店");
                return java.util.Map.of(
                        "message", "暂无需要同步的门店",
                        "totalCount", 0,
                        "successCount", 0,
                        "failCount", 0,
                        "completed", true);
            }

            EleOrderSyncTaskExecutor.SyncResult result = syncTaskExecutor.executeSync(stores, null, null);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("elapsedSeconds", result.getElapsedSeconds());
            response.put("completed", result.isCompleted());
            response.put("failedStores", result.getFailedStores());

            log.info("【手动全量同步-详细】同步完成：总{}家，成功{}家，失败{}家，耗时{}秒",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailCount(),
                    result.getElapsedSeconds());

            return response;
        } finally {
            shutdownStateManager.finishBatchSync();
        }
    }

    private boolean isTerminalOrder(OrderListRespDTO.OrderDetail orderDetail) {
        return orderDetail != null && isTerminalStatus(orderDetail.getStatus());
    }

    private boolean isTerminalStatus(Integer status) {
        return EleOrderStatusEnum.isTerminalStatus(status);
    }

    private Long parseStoreId(String erpStoreCode) {
        if (erpStoreCode == null || erpStoreCode.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(erpStoreCode);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<OrderDetailRespDTO.SubOrder> convertItems(List<OrderItemDO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        return items.stream().map(item -> {
            OrderDetailRespDTO.SubOrder subOrder = new OrderDetailRespDTO.SubOrder();
            subOrder.setSubOrderId(item.getSubOrderId());
            subOrder.setSkuCode(item.getSkuCode());
            subOrder.setSkuName(item.getSkuName());
            subOrder.setBarcode(item.getBarcode());
            subOrder.setSpecification(item.getSpecification());
            subOrder.setWeight(item.getWeight() == null ? null : item.getWeight().intValue());
            subOrder.setBuyAmount(item.getBuyAmount());
            subOrder.setNum(item.getNum());
            subOrder.setPrice(yuanToFen(item.getPrice()));
            subOrder.setTotalFee(yuanToFen(item.getTotalFee()));
            subOrder.setPayFee(yuanToFen(item.getPayFee()));
            subOrder.setGoodsType(convertProductType(item.getProductType()));
            return subOrder;
        }).collect(Collectors.toList());
    }

    private List<OrderDetailRespDTO.Discount> convertDiscounts(List<OrderDiscountDO> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return new ArrayList<>();
        }
        return discounts.stream().map(discount -> {
            OrderDetailRespDTO.Discount dto = new OrderDetailRespDTO.Discount();
            dto.setActivityName(discount.getActivityName());
            dto.setActivityId(discount.getActivityId());
            dto.setType(discount.getDiscountType());
            dto.setDiscountFee(yuanToFen(discount.getDiscountFee()));
            dto.setMerchantFee(yuanToFen(discount.getMerchantFee()));
            dto.setPlatformFee(yuanToFen(discount.getPlatformFee()));
            return dto;
        }).collect(Collectors.toList());
    }

    private String resolveStoreCode(OrderListRespDTO.OrderDetail detail, String erpStoreCode) {
        if (erpStoreCode != null && !erpStoreCode.isEmpty()) {
            return erpStoreCode;
        }
        if (detail.getErpStoreCode() != null && !detail.getErpStoreCode().isEmpty()) {
            return detail.getErpStoreCode();
        }
        return detail.getStoreCode();
    }

    private BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(new BigDecimal("100"));
    }

    private BigDecimal calculateTotalWeight(Integer weight, Integer buyAmount) {
        if (weight == null || buyAmount == null) {
            return null;
        }
        return new BigDecimal(weight).multiply(new BigDecimal(buyAmount))
                .divide(new BigDecimal("1000"));
    }

    private String convertProductType(Integer productType) {
        if (productType == null) {
            return null;
        }
        return switch (productType) {
            case 0 -> "0";
            case 1 -> "3";
            default -> null;
        };
    }

    private Integer yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(new BigDecimal("100")).intValue();
    }

    private String getUserId(String userId) {
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        return "32318038";
    }

    private OrderDetailRespDTO convertDetailResult(SaasOrderGetResult result) {
        SaasOrderGetResult.SaasOrderGetData data = result.getData();
        if (data == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        OrderDetailRespDTO dto = new OrderDetailRespDTO();
        fillOrderDetail(dto, data);

        return dto;
    }

    private void enrichOrderListWithDetails(OrderListRespDTO listResult, OrderListReqDTO req, String merchantCode,
            String erpStoreCode) {
        if (listResult == null || listResult.getOrderList() == null || listResult.getOrderList().isEmpty()) {
            return;
        }

        for (int i = 0; i < listResult.getOrderList().size(); i++) {
            OrderListRespDTO.OrderDetail summary = listResult.getOrderList().get(i);
            if (summary == null || summary.getOrderId() == null || summary.getOrderId().isEmpty()) {
                continue;
            }

            try {
                OrderDetailRespDTO detail = getOrderDetailRemote(req.getPlatformStoreId(), merchantCode, erpStoreCode,
                        summary.getOrderId());
                if (detail != null) {
                    listResult.getOrderList().set(i, convertToOrderListDetail(detail));
                    updatePlatformDeliveryStatus(detail.getOrderId(), detail.getDeliveryStatus());
                }
            } catch (Exception e) {
                log.warn("【详情查询失败】orderId={}, 保留基础信息, error={}", summary.getOrderId(), e.getMessage());
                saveFailRecord(summary.getOrderId(), null, "DETAIL", "API", e.getMessage(), summary, null, 0, null,
                        req.getPlatformStoreId(), merchantCode, erpStoreCode);
            }
        }
    }

    private void fillOrderDetail(OrderDetailRespDTO dto, SaasOrderGetResult.SaasOrderGetData data) {
        dto.setOrderId(data.getOrder_id());
        dto.setCreateTime(data.getCreate_time());
        dto.setPayTime(data.getPay_time());
        dto.setChannelSourceName(data.getChannel_source_name());
        dto.setBuyerName(data.getBuyer_name());
        dto.setBuyerPhone(data.getBuyer_phone());
        dto.setBuyerAddress(data.getBuyer_address());
        dto.setDeliveryName(data.getDelivery_name());
        dto.setDeliveryPhone(data.getDelivery_phone());
        dto.setDeliveryPlatform(data.getDelivery_platform());
        dto.setDeliveryType(data.getDelivery_type());
        dto.setDeliveryStatus(data.getDelivery_status());
        dto.setStatus(data.getStatus());
        dto.setTotalFee(data.getTotal_fee());
        dto.setPayFee(data.getPay_fee());
        dto.setDiscountFee(data.getDiscount_fee());
        dto.setDeliveryFee(data.getDelivery_fee());
        dto.setPostFee(data.getPost_fee());
        dto.setPackageFee(data.getPackage_fee());
        dto.setPlatformCommissionFee(data.getPlatform_commission_fee());
        dto.setRemark(data.getRemark());
        dto.setChannelSourceId(data.getChannel_source_id());
        dto.setChannelOrderId(data.getChannel_order_id());
        dto.setChannelType(data.getChannel_type());
        dto.setStoreCode(data.getStore_code());
        dto.setErpStoreCode(data.getErp_store_code());
        dto.setLongitude(data.getLongitude());
        dto.setLatitude(data.getLatitude());
        dto.setArriveType(data.getArrive_type());

        if (data.getSub_orders() != null) {
            for (SaasOrderGetResult.SubOrder subOrder : data.getSub_orders()) {
                OrderDetailRespDTO.SubOrder subOrderDTO = new OrderDetailRespDTO.SubOrder();
                subOrderDTO.setSubOrderId(subOrder.getSub_order_id());
                subOrderDTO.setSkuCode(subOrder.getSku_code());
                subOrderDTO.setSkuName(subOrder.getSku_name());
                subOrderDTO.setBarcode(subOrder.getBarcode());
                subOrderDTO.setSpecification(subOrder.getSpecification());
                subOrderDTO.setPrice(subOrder.getPrice());
                subOrderDTO.setTotalFee(subOrder.getTotal_fee());
                subOrderDTO.setPayFee(subOrder.getPay_fee());
                subOrderDTO.setBuyAmount(subOrder.getBuy_amount());
                subOrderDTO.setGoodsType(subOrder.getGoods_type());
                subOrderDTO.setCabinetCode(subOrder.getCabinet_code());
                subOrderDTO.setWeight(subOrder.getWeight());
                subOrderDTO.setNum(subOrder.getNum());
                dto.getSubOrders().add(subOrderDTO);
            }
        }

        if (data.getDiscounts() != null) {
            for (SaasOrderGetResult.Discount discount : data.getDiscounts()) {
                OrderDetailRespDTO.Discount discountDTO = new OrderDetailRespDTO.Discount();
                discountDTO.setActivityName(discount.getActivity_name());
                discountDTO.setActivityOrderType(discount.getActivity_order_type());
                discountDTO.setActivityId(discount.getActivity_id());
                discountDTO.setType(discount.getType());
                discountDTO.setDiscountFee(discount.getDiscount_fee());
                discountDTO.setMerchantFee(discount.getMerchant_fee());
                discountDTO.setPlatformFee(discount.getPlatform_fee());
                if (log.isDebugEnabled()) {
                    log.debug("【优惠数据】orderId={}, activityName={}, activityId={}",
                            data.getOrder_id(), discount.getActivity_name(), discount.getActivity_id());
                }
                dto.getDiscounts().add(discountDTO);
            }
        }
    }

    private EleApiConfig getApiConfig() {
        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null) {
            throw new RuntimeException("未找到翱象API配置，请检查 ele_api_config 表中是否存在 status=1 的记录");
        }
        return config;
    }

    private ApiExecutor<SaasOrderListResult> buildExecutor(EleApiConfig config) {
        return new ApiExecutor<>(config.getAppId(), config.getAppSecret());
    }

    private ApiExecutor<SaasOrderGetResult> buildDetailExecutor(EleApiConfig config) {
        if (config == null) {
            throw new RuntimeException("API配置为null");
        }
        String appId = config.getAppId();
        String appSecret = config.getAppSecret();
        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("API配置中appId为空，配置ID=" + config.getId());
        }
        if (appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("API配置中appSecret为空，配置ID=" + config.getId());
        }
        return new ApiExecutor<>(appId, appSecret);
    }

    private OrderListRespDTO convertListResult(BizResultWrapper<SaasOrderListResult> wrapper) {
        if (wrapper == null || wrapper.getBody() == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        SaasOrderListResult result = wrapper.getBody();
        String errno = result.getErrno();
        if (errno != null && !"0".equals(errno)) {
            throw new RuntimeException("翱象接口返回错误[" + errno + "]: " + result.getError());
        }

        OrderListRespDTO dto = new OrderListRespDTO();
        MeEleRetailSaasOrderListResDto data = result.getData();
        if (data == null) {
            dto.setOrderList(new ArrayList<>());
            return dto;
        }

        dto.setTotal(data.getTotal());
        dto.setScrollId(data.getScroll_id());

        MeEleRetailSaasOrderListDetailResDto[] orderArr = data.getOrder_list();
        if (orderArr != null) {
            for (MeEleRetailSaasOrderListDetailResDto item : orderArr) {
                if (item.getStatus() != null && item.getStatus() == -2) {
                    continue;
                }

                OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
                detail.setOrderId(item.getOrder_id());
                detail.setStatus(item.getStatus());
                detail.setCreateTime(item.getCreate_time());
                dto.getOrderList().add(detail);
            }
        }

        return dto;
    }

    private OrderDetailRespDTO getOrderDetailRemote(String platformStoreId, String merchantCode, String erpStoreCode,
            String orderId) {
        EleApiConfig config = getApiConfig();
        log.info("【调用翱象订单详情接口】orderId={}, platformStoreId={}, merchantCode={}, erpStoreCode={}, config.appId={}",
                orderId, platformStoreId, merchantCode, erpStoreCode, config.getAppId());

        if (orderId == null || orderId.isEmpty()) {
            throw new RuntimeException("orderId不能为空");
        }

        String finalMerchantCode = merchantCode;
        String finalErpStoreCode = erpStoreCode;
        if (platformStoreId != null && !platformStoreId.isEmpty()) {
            StorePlatformRespVO platformInfo = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
            if (platformInfo == null) {
                throw new RuntimeException("未找到平台门店ID对应的门店信息: " + platformStoreId);
            }
            finalMerchantCode = platformInfo.getSettlementAccount();
            finalErpStoreCode = platformStoreId;
            log.info("【门店信息查询成功】platformStoreId={}, settlementAccount={}", platformStoreId, finalMerchantCode);
        }

        if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
            finalMerchantCode = config.getMerchantCode();
        }
        if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
            throw new RuntimeException("merchantCode不能为空");
        }
        if (finalErpStoreCode == null || finalErpStoreCode.isEmpty()) {
            throw new RuntimeException("erpStoreCode不能为空");
        }

        SaasOrderGetParam.SaasOrderGetBody body = new SaasOrderGetParam.SaasOrderGetBody();
        body.setOrder_id(orderId);
        body.setMerchant_code(finalMerchantCode);
        body.setErp_store_code(finalErpStoreCode);

        SaasOrderGetParam param = new SaasOrderGetParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());
        param.setEncrypt("aes");
        param.setBody(body);

        log.info("【请求参数】orderId={}, merchantCode={}, erpStoreCode={}", orderId, finalMerchantCode, finalErpStoreCode);

        ORDER_GET_RATE_LIMITER.acquire();
        try {
            BizResultWrapper<SaasOrderGetResult> wrapper = eleOpenApiClient.sendOrderDetail(config, param,
                    finalMerchantCode, platformStoreId, finalErpStoreCode, orderId);

            if (wrapper == null) {
                throw new RuntimeException("翱象接口返回wrapper为null");
            }
            if (wrapper.getBody() == null) {
                throw new RuntimeException("翱象接口返回body为null");
            }
            SaasOrderGetResult result = wrapper.getBody();

            String errno = result.getErrno();
            if (errno != null && !"0".equals(errno)) {
                throw new RuntimeException("翱象接口返回错误[" + errno + "]: " + result.getError());
            }

            if (result.getData() == null) {
                throw new RuntimeException("翱象接口返回data为null, orderId=" + orderId);
            }

            OrderDetailRespDTO dto = convertDetailResult(result);
            if (dto == null) {
                throw new RuntimeException("转换详情结果为null, orderId=" + orderId);
            }
            if (isTerminalStatus(dto.getStatus())) {
                saveRemoteOrderDetailToLocal(dto, finalErpStoreCode);
            } else {
                log.info("【跳过详情落库】远程详情非目标终态，orderId={}, status={}", dto.getOrderId(), dto.getStatus());
            }

            return dto;
        } catch (Exception e) {
            saveFailRecord(orderId, null, "DETAIL", "API", e.getMessage(), param, null, 0, null,
                    platformStoreId, merchantCode, erpStoreCode);
            throw new RuntimeException("调用翱象订单详情接口失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> getUnhandledFailCount() {
        long pendingRetryCount = eleOrderFailRecordMapper.selectCount(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "PENDING_RETRY"));

        long failedCount = eleOrderFailRecordMapper.selectCount(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "FAILED"));

        Map<String, Integer> result = new HashMap<>();
        result.put("totalUnhandleCount", (int) (pendingRetryCount + failedCount));
        result.put("pendingRetryCount", (int) pendingRetryCount);
        result.put("failedCount", (int) failedCount);
        return result;
    }

    @Override
    public List<Long> getAllFailedIds() {
        List<EleOrderFailRecord> records = eleOrderFailRecordMapper.selectList(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "FAILED"));
        return records.stream()
                .map(EleOrderFailRecord::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public int retryAllFailedRecordsBySpecifiedTime(Long specifiedTime, Boolean overwrite) {
        if (specifiedTime == null) {
            throw new IllegalArgumentException("指定时间点不能为空");
        }

        // 查询所有 FAILED 状态的失败记录（不限时间范围）
        List<EleOrderFailRecord> records = eleOrderFailRecordMapper.selectList(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "FAILED")
                        .orderByAsc(EleOrderFailRecord::getCreateTime));

        if (records.isEmpty()) {
            log.info("【指定时间点重试】未找到任何 FAILED 状态的失败记录，specifiedTime={}", specifiedTime);
            return 0;
        }

        log.info("【指定时间点重试】找到 {} 条 FAILED 状态的失败记录，specifiedTime={}, overwrite={}",
                records.size(), specifiedTime, overwrite);

        int successCount = 0;
        for (EleOrderFailRecord record : records) {
            try {
                retryFailRecord(record.getId(), overwrite);
                successCount++;
            } catch (Exception e) {
                log.error("【指定时间点重试】重试失败记录失败，recordId={}, orderId={}, error={}",
                        record.getId(), record.getOrderId(), e.getMessage());
            }
        }

        log.info("【指定时间点重试】完成，总数={}, 成功={}, 失败={}", records.size(), successCount, records.size() - successCount);
        return successCount;
    }

    @Override
    public Map<String, Object> getSyncConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("syncIntervalMinutes", 15);
        config.put("syncIntervalMs", 15 * 60 * 1000);
        config.put("syncCron", "0 */15 * * * ?");
        return config;
    }
}
