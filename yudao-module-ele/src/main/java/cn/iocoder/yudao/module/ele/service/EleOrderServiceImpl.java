package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderFailRecordMapper;
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
import cn.iocoder.yudao.module.ele.service.dto.FailedOrderInfo;
import cn.iocoder.yudao.module.ele.service.dto.CompensationResult;
import cn.iocoder.yudao.module.ele.service.dto.CompensationTaskResult;
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
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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
    @Resource(name = "eleOrderRetryExecutor")
    private ThreadPoolTaskExecutor retryExecutor;
    @Resource
    private EleOrderRetryTaskSubmitter retryTaskSubmitter;
    @Resource
    private EleOpenApiClient eleOpenApiClient;
    @Resource
    private ShutdownStateManager shutdownStateManager;
    @Resource
    private EleOrderTrackingService eleOrderTrackingService;

    @Value("${ele.order.sync.window.min-count:10}")
    private int windowMinCount;

    @Value("${ele.order.sync.window.target-per-window:5000}")
    private int windowTargetPerWindow;

    @Value("${ele.order.sync.window.max-windows:12}")
    private int windowMaxWindows;

    @Value("${ele.order.sync.window.overlap-seconds:1}")
    private long windowOverlapSeconds;

    @Value("${ele.order.sync.window.lock-lease-minutes:30}")
    private int windowLockLeaseMinutes;

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

            long stuckThreshold = System.currentTimeMillis() - 1 * 60 * 1000;
            List<EleOrderFailRecord> stuckRetryingRecords = eleOrderFailRecordMapper.selectList(
                    new LambdaQueryWrapperX<EleOrderFailRecord>()
                            .eq(EleOrderFailRecord::getProcessStatus, "RETRYING")
                            .lt(EleOrderFailRecord::getRetryCount, 3)
                            .lt(EleOrderFailRecord::getUpdateTime, stuckThreshold)
                            .orderByAsc(EleOrderFailRecord::getCreateTime)
                            .last("LIMIT 100"));

            List<EleOrderFailRecord> exhaustedRetryingRecords = eleOrderFailRecordMapper.selectList(
                    new LambdaQueryWrapperX<EleOrderFailRecord>()
                            .eq(EleOrderFailRecord::getProcessStatus, "RETRYING")
                            .ge(EleOrderFailRecord::getRetryCount, 3)
                            .lt(EleOrderFailRecord::getUpdateTime, stuckThreshold)
                            .orderByAsc(EleOrderFailRecord::getCreateTime)
                            .last("LIMIT 100"));

            if (pendingRecords == null) {
                pendingRecords = new ArrayList<>();
            }
            if (exhaustedRetryingRecords != null && !exhaustedRetryingRecords.isEmpty()) {
                log.info("【定时扫描】发现{}条RETRYING重试耗尽记录（已重试≥3次且超时），退回FAILED",
                        exhaustedRetryingRecords.size());
                for (EleOrderFailRecord exhausted : exhaustedRetryingRecords) {
                    exhausted.setProcessStatus("FAILED");
                    exhausted.setRemark("已重试" + exhausted.getRetryCount() + "次仍失败，退回FAILED等待人工处理");
                    exhausted.setUpdateTime(System.currentTimeMillis());
                    eleOrderFailRecordMapper.updateById(exhausted);
                }
            }
            if (stuckRetryingRecords != null && !stuckRetryingRecords.isEmpty()) {
                log.info("【定时扫描】发现{}条RETRYING超时记录（超过1分钟未更新），回退为PENDING_RETRY重新提交",
                        stuckRetryingRecords.size());
                for (EleOrderFailRecord stuck : stuckRetryingRecords) {
                    stuck.setProcessStatus("PENDING_RETRY");
                    stuck.setRemark("RETRYING超时，自动回退为PENDING_RETRY");
                    stuck.setUpdateTime(System.currentTimeMillis());
                    eleOrderFailRecordMapper.updateById(stuck);
                }
                pendingRecords.addAll(stuckRetryingRecords);
            }

            if (pendingRecords.isEmpty()) {
                return;
            }

            log.info("【定时扫描】发现{}条待重试记录，准备去重提交", pendingRecords.size());

            // 去重: 尝试将状态更新为RETRYING，失败则跳过(已被其他路径处理)
            List<EleOrderFailRecord> dedupedRecords = new ArrayList<>();
            for (EleOrderFailRecord record : pendingRecords) {
                try {
                    EleOrderFailRecord updatedRecord = new EleOrderFailRecord();
                    updatedRecord.setId(record.getId());
                    updatedRecord.setProcessStatus("RETRYING");
                    updatedRecord.setUpdateTime(System.currentTimeMillis());
                    int updated = eleOrderFailRecordMapper.update(updatedRecord,
                            new LambdaQueryWrapperX<EleOrderFailRecord>()
                                    .eq(EleOrderFailRecord::getId, record.getId())
                                    .eq(EleOrderFailRecord::getProcessStatus, "PENDING_RETRY"));
                    if (updated > 0) {
                        record.setProcessStatus("RETRYING");
                        dedupedRecords.add(record);
                    } else {
                        log.info("【重试去重】订单已被其他路径处理，跳过，orderId={}", record.getOrderId());
                    }
                } catch (Exception e) {
                    log.warn("【重试去重】更新状态失败，跳过，orderId={}", record.getOrderId());
                }
            }

            if (dedupedRecords.isEmpty()) {
                log.info("【定时扫描】去重后无待重试记录");
                return;
            }

            log.info("【定时扫描】去重后{}条记录，准备提交Kafka", dedupedRecords.size());

            for (EleOrderFailRecord record : dedupedRecords) {
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

                    record.setProcessStatus("RETRYING");
                    record.setUpdateTime(System.currentTimeMillis());
                    eleOrderFailRecordMapper.updateById(record);

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
        if (shutdownStateManager.isShuttingDown()) {
            log.warn("【订单同步】应用正在关闭，跳过门店同步，platformStoreId={}", store.getPlatformStoreId());
            throw new EleOrderSyncException("【订单同步】应用正在关闭，跳过门店同步，platformStoreId=" + store.getPlatformStoreId());
        }

        String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
        String merchantCode = StrUtil.trim(store.getSettlementAccount());
        String erpStoreCode = platformStoreId;
        String storeName = store.getPlatformStoreName();
        LocalDateTime syncStartTime = LocalDateTime.now();
        String syncBatchId = UUID.randomUUID().toString().replace("-", "");

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
            long totalCount = getTotalCountByStatus(platformStoreId, merchantCode, erpStoreCode, startTime, endTime);
            log.info("【动态窗口】门店{}试探完成，时间范围[{}-{}]内总订单数: {}条",
                    platformStoreId, startTime, endTime, totalCount);

            int windowCount = calculateWindowCount(totalCount);
            log.info("【动态窗口】门店{}总数据量{}条，拆分成{}个窗口", platformStoreId, totalCount, windowCount);

            List<OrderListRespDTO.OrderDetail> allOrders;
            if (windowCount == 1) {
                log.info("【动态窗口】门店{}数据量<{}条，不拆分，直接串行拉取", platformStoreId, windowMinCount);
                allOrders = pullAllOrders(platformStoreId, merchantCode, erpStoreCode, startTime, endTime);
            } else {
                log.info("【动态窗口】门店{}数据量>=10条，启动{}个窗口并行拉取", platformStoreId, windowCount);
                allOrders = pullAllOrdersWithWindows(platformStoreId, merchantCode, erpStoreCode, startTime, endTime,
                        windowCount);
            }

            List<FailedOrderInfo> failedOrders = Collections.synchronizedList(new ArrayList<>());
            Long lastSuccessTimestamp = saveOrUpdateBatchWithFailureTracking(allOrders, platformStoreId, merchantCode,
                    erpStoreCode, failedOrders);

            CompensationResult compResult = executeCompensationTasks(failedOrders, platformStoreId, merchantCode,
                    erpStoreCode);

            handleCompensationResults(compResult);

            submitPendingRetries(platformStoreId, merchantCode, erpStoreCode);

            Long finalSyncTime = (lastSuccessTimestamp != null && lastSuccessTimestamp > startTime)
                    ? lastSuccessTimestamp
                    : startTime;

            long successCount = allOrders.stream().filter(o -> o.getStatus() != null).count();
            int failCount = (int) (allOrders.size() - successCount);
            LocalDateTime syncEndTime = LocalDateTime.now();

            List<OrderListRespDTO.OrderDetail> kafkaOrders = allOrders.stream()
                    .filter(o -> o.getStatus() != null)
                    .collect(Collectors.toList());

            boolean allCompensationSuccess = compResult == null || compResult.isAllSuccess();
            int compensationFailCount = compResult != null ? compResult.getFailedCount() : 0;

            EleOrderSyncLog existingLog = eleOrderSyncLogMapper.selectByStoreId(platformStoreId);
            if (existingLog != null) {
                existingLog.setSyncBatchId(syncBatchId);
                existingLog.setMerchantCode(merchantCode);
                existingLog.setErpStoreCode(erpStoreCode);
                existingLog.setStoreName(storeName);
                existingLog.setLastSyncTime(startTime);
                existingLog.setSyncStartTime(syncStartTime);
                existingLog.setSyncEndTime(syncEndTime);
                existingLog.setSyncTime(finalSyncTime);
                existingLog.setSyncCount(allOrders.size());
                existingLog.setSuccessCount((int) successCount);
                existingLog.setFailCount(failCount);
                if (allCompensationSuccess) {
                    existingLog.setStatus(1);
                    existingLog.setPartialFailed(0);
                    existingLog.setErrorMsg(null);
                } else {
                    existingLog.setStatus(0);
                    existingLog.setPartialFailed(1);
                    existingLog.setErrorMsg("补偿任务完成后仍有" + compensationFailCount + "个订单失败");
                }
                eleOrderSyncLogMapper.updateById(existingLog);
            } else {
                EleOrderSyncLog syncLog = new EleOrderSyncLog();
                syncLog.setSyncBatchId(syncBatchId);
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
                if (allCompensationSuccess) {
                    syncLog.setStatus(1);
                    syncLog.setPartialFailed(0);
                } else {
                    syncLog.setStatus(0);
                    syncLog.setPartialFailed(1);
                    syncLog.setErrorMsg("补偿任务完成后仍有" + compensationFailCount + "个订单失败");
                }
                syncLog.setCreateTime(System.currentTimeMillis());
                eleOrderSyncLogMapper.insert(syncLog);
            }

            log.info("【syncTime推进】门店{}，startTime={}，endTime={}，lastSuccess={}，finalSyncTime={}",
                    platformStoreId, startTime, endTime, lastSuccessTimestamp, finalSyncTime);

            shutdownStateManager.addOrderCounts(allOrders.size(), (int) successCount, failCount);

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
                existingLog.setSyncBatchId(syncBatchId);
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
                failLog.setSyncBatchId(syncBatchId);
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
    }

    private void runSyncCycle(Long forcedStartTime, Long forcedEndTime) {
        List<StorePlatformRespVO> stores = storeService.getAllPlatformStoresByPlatformCode(null);
        List<StorePlatformRespVO> validStores = stores == null ? List.of()
                : stores.stream()
                        .filter(s -> cn.hutool.core.util.StrUtil.isNotBlank(s.getPlatformStoreId()))
                        .toList();

        if (!shutdownStateManager.startBatchSync(validStores.size())) {
            log.warn("【订单同步】已有批次在执行或应用正在关闭，跳过本次同步");
            return;
        }
        try {
            if (validStores.isEmpty()) {
                log.info("暂无需要同步的门店");
                shutdownStateManager.finishBatchSync();
                return;
            }

            syncTaskExecutor.executeSync(validStores, forcedStartTime, forcedEndTime);
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
            throw new ServiceException(400, "失败记录不存在");
        }
        if ("SUCCESS".equals(record.getProcessStatus())) {
            throw new ServiceException(400, "该失败记录已成功处理，无需重试");
        }
        if ("RETRYING".equals(record.getProcessStatus())) {
            throw new ServiceException(400, "该失败记录正在重试中，请勿重复操作，请稍后再试");
        }
        if ("PENDING_MANUAL".equals(record.getProcessStatus())) {
            throw new ServiceException(400, "该失败记录已标记为需要人工处理，请检查门店信息配置后重试");
        }

        boolean hasOrderId = record.getOrderId() != null && !record.getOrderId().isEmpty();
        boolean hasStoreInfo = record.getPlatformStoreId() != null && !record.getPlatformStoreId().isEmpty()
                && record.getMerchantCode() != null && !record.getMerchantCode().isEmpty()
                && record.getErpStoreCode() != null && !record.getErpStoreCode().isEmpty();

        if (hasOrderId && !hasStoreInfo) {
            OrderDO existingOrder = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                    .eq(OrderDO::getOrderId, record.getOrderId())
                    .eq(OrderDO::getDeleted, false)
                    .last("LIMIT 1"))
                    .stream().findFirst().orElse(null);
            if (existingOrder != null && StrUtil.isNotBlank(existingOrder.getStoreCode())) {
                StorePlatformRespVO platformInfo = storeService
                        .getPlatformTableByPlatformStoreId(existingOrder.getStoreCode());
                if (platformInfo != null && StrUtil.isNotBlank(platformInfo.getPlatformStoreId())) {
                    log.info("【重试补全门店信息】从order_table+storeService补全，orderId={}, storeCode={}, platformStoreId={}",
                            record.getOrderId(), existingOrder.getStoreCode(), platformInfo.getPlatformStoreId());
                    record.setPlatformStoreId(platformInfo.getPlatformStoreId());
                    record.setMerchantCode(platformInfo.getSettlementAccount());
                    record.setErpStoreCode(platformInfo.getPlatformStoreId());
                    hasStoreInfo = true;
                }
            }
            if (!hasStoreInfo) {
                StorePlatformRespVO platformInfo = storeService
                        .getPlatformTableByPlatformStoreId(record.getOrderId());
                if (platformInfo != null && StrUtil.isNotBlank(platformInfo.getPlatformStoreId())) {
                    log.info("【重试补全门店信息】通过storeService直接补全，orderId={}", record.getOrderId());
                    record.setPlatformStoreId(platformInfo.getPlatformStoreId());
                    record.setMerchantCode(platformInfo.getSettlementAccount());
                    record.setErpStoreCode(platformInfo.getPlatformStoreId());
                    hasStoreInfo = true;
                }
            }
            if (!hasStoreInfo && StrUtil.isNotBlank(record.getRequestParam())) {
                try {
                    com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(record.getRequestParam());
                    String storeCodeFromParam = null;
                    if (jsonNode.has("storeCode")) {
                        storeCodeFromParam = jsonNode.get("storeCode").asText();
                    } else if (jsonNode.has("platformStoreId")) {
                        storeCodeFromParam = jsonNode.get("platformStoreId").asText();
                    } else if (jsonNode.has("erpStoreCode")) {
                        storeCodeFromParam = jsonNode.get("erpStoreCode").asText();
                    }
                    if (StrUtil.isNotBlank(storeCodeFromParam)) {
                        StorePlatformRespVO platformInfo = storeService
                                .getPlatformTableByPlatformStoreId(storeCodeFromParam);
                        if (platformInfo != null && StrUtil.isNotBlank(platformInfo.getPlatformStoreId())) {
                            log.info("【重试补全门店信息】从requestParam解析，orderId={}, storeCode={}",
                                    record.getOrderId(), storeCodeFromParam);
                            record.setPlatformStoreId(platformInfo.getPlatformStoreId());
                            record.setMerchantCode(platformInfo.getSettlementAccount());
                            record.setErpStoreCode(platformInfo.getPlatformStoreId());
                            hasStoreInfo = true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("【解析requestParam失败】recordId={}, error={}", record.getId(), e.getMessage());
                }
            }
        }

        if (hasOrderId && hasStoreInfo) {
            retryFailRecordForSingleOrder(record, overwrite);
        } else if (!hasOrderId && hasStoreInfo) {
            retryFailRecordForStoreSync(record);
        } else {
            record.setProcessStatus("PENDING_MANUAL");
            if (hasOrderId) {
                record.setRemark("无法自动重试：有orderId但缺少门店信息，且order_table中未找到对应记录，等待人工处理");
            } else {
                record.setRemark("无法自动重试：缺少orderId和门店信息，等待人工处理");
            }
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
            String detail = hasOrderId ? "有orderId但缺少门店信息" : "缺少orderId和门店信息";
            throw new ServiceException(400, "失败记录" + detail + "，无法自动重试，已标记为人工处理，记录ID=" + id);
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

        // 拉取全部状态的订单
        Integer[] targetStatuses = { 1, 2, 3, 4, 5, 6, -1 };

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

        log.info("【拉取订单】platformStoreId={}, 共{}条（全部状态）", platformStoreId, originalCount);
        for (OrderListRespDTO.OrderDetail order : allOrders) {
            log.info("【拉取订单】订单详情: orderId={}, channelOrderId={}, status={}, deliveryStatus={}, totalFee={}, payFee={}",
                    order.getOrderId(), order.getChannelOrderId(), order.getStatus(), order.getDeliveryStatus(),
                    order.getTotalFee(), order.getPayFee());
        }

        return allOrders;
    }

    @Override
    public PageResult<OrderListRespDTO.OrderDetail> getOrdersFromLocal(String platformStoreId, String storeId,
            Integer status,
            Long startTime, Long endTime, Integer pageNo, Integer pageSize) {
        List<String> storeCodes = new ArrayList<>();

        if (StrUtil.isNotBlank(storeId)) {
            List<StorePlatformRespVO> platforms = storeService.getPlatformTableListByStoreId(storeId);
            if (CollUtil.isNotEmpty(platforms)) {
                for (StorePlatformRespVO p : platforms) {
                    if (StrUtil.isNotBlank(p.getPlatformStoreId())) {
                        storeCodes.add(p.getPlatformStoreId());
                    }
                }
            }
            storeCodes.add(storeId);
        } else if (StrUtil.isNotBlank(platformStoreId)) {
            storeCodes.add(platformStoreId);
        }

        if (startTime != null && startTime < 100000000000L) {
            startTime = startTime * 1000;
        }
        if (endTime != null && endTime < 100000000000L) {
            endTime = endTime * 1000;
        }

        List<OrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<OrderDO>()
                .in(CollUtil.isNotEmpty(storeCodes), OrderDO::getStoreCode, storeCodes)
                .eq(status != null, OrderDO::getOrderStatus, status)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false)
                .orderByDesc(OrderDO::getCreateTime)
                .last("LIMIT " + ((pageNo - 1) * pageSize) + ", " + pageSize));

        Long total = orderMapper.selectCount(new LambdaQueryWrapperX<OrderDO>()
                .in(CollUtil.isNotEmpty(storeCodes), OrderDO::getStoreCode, storeCodes)
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

        // 1. 保存全部订单（所有状态）
        List<OrderListRespDTO.OrderDetail> allValidOrders = orders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.toList());

        if (allValidOrders.isEmpty()) {
            return null;
        }

        int totalCount = allValidOrders.size();
        int threadCount = calculateWriteThreadCount(totalCount);

        log.info("【订单保存】开始保存{}个订单（全部状态），动态分配{}线程", totalCount, threadCount);

        List<FailedOrderInfo> failedOrders = new ArrayList<>();
        if (threadCount <= 1 || totalCount <= 50) {
            return saveOrdersSingleThread(allValidOrders, platformStoreId, merchantCode, erpStoreCode, failedOrders);
        } else {
            return saveOrdersMultiThread(allValidOrders, platformStoreId, merchantCode, erpStoreCode, threadCount,
                    failedOrders);
        }
    }

    /**
     * 根据订单量计算写入线程数
     */
    int calculateWriteThreadCount(int orderCount) {
        if (orderCount < 100)
            return 1;
        if (orderCount < 500)
            return 2;
        if (orderCount < 2000)
            return 3;
        return 4;
    }

    /**
     * 单线程保存订单（小数据量场景）
     * 失败时：记录到内存列表+提交Kafka重试，不写DB
     */
    private Long saveOrdersSingleThread(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, List<FailedOrderInfo> failedOrders) {
        int successCount = 0;
        List<String> failedOrderIds = new ArrayList<>();
        Long maxCreateTime = null;
        Long minFailedCreateTime = null;

        for (OrderListRespDTO.OrderDetail order : orders) {
            try {
                upsertOrder(order, platformStoreId, merchantCode, erpStoreCode);
                successCount++;

                if (order.getCreateTime() != null) {
                    if (maxCreateTime == null || order.getCreateTime() > maxCreateTime) {
                        maxCreateTime = order.getCreateTime();
                    }
                }
            } catch (Exception e) {
                log.error("【订单保存失败】orderId={}, error={}", order.getOrderId(), e.getMessage());
                failedOrderIds.add(order.getOrderId());

                if (order.getCreateTime() != null) {
                    if (minFailedCreateTime == null || order.getCreateTime() < minFailedCreateTime) {
                        minFailedCreateTime = order.getCreateTime();
                    }
                }

                FailedOrderInfo failedInfo = new FailedOrderInfo();
                failedInfo.setOrderId(order.getOrderId());
                failedInfo.setChannelOrderId(order.getChannelOrderId());
                failedInfo.setPlatformStoreId(platformStoreId);
                failedInfo.setMerchantCode(merchantCode);
                failedInfo.setErpStoreCode(erpStoreCode);
                failedInfo.setErrorMessage(e.getMessage());
                failedInfo.setFailTimestamp(System.currentTimeMillis());
                failedInfo.setOrderDetail(order);
                failedInfo.setRetryCount(0);
                failedOrders.add(failedInfo);

                EleOrderRetryTaskSubmitter.RetryTask retryTask = new EleOrderRetryTaskSubmitter.RetryTask(
                        order.getOrderId(), order.getChannelOrderId(),
                        platformStoreId, merchantCode, erpStoreCode,
                        null, order);
                List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
                tasks.add(retryTask);
                retryTaskSubmitter.submitRetryTasks(tasks);
            }
        }

        Long finalSyncTime = calculateSyncTime(maxCreateTime, minFailedCreateTime, successCount, orders.size());

        updateSyncLogWithFailedDetails(platformStoreId, merchantCode, erpStoreCode,
                orders.size(), successCount, failedOrderIds, finalSyncTime, null);

        return maxCreateTime;
    }

    /**
     * 多线程保存订单（大数据量场景）
     * 失败时：记录到内存列表+提交Kafka重试，不写DB
     */
    private Long saveOrdersMultiThread(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, int threadCount, List<FailedOrderInfo> failedOrders) {
        int totalOrders = orders.size();
        List<List<OrderListRespDTO.OrderDetail>> partitions = new ArrayList<>();
        int partitionSize = (totalOrders + threadCount - 1) / threadCount;

        for (int i = 0; i < totalOrders; i += partitionSize) {
            partitions.add(orders.subList(i, Math.min(i + partitionSize, totalOrders)));
        }

        AtomicInteger totalSuccess = new AtomicInteger(0);
        List<String> allFailedOrderIds = new CopyOnWriteArrayList<>();
        List<FailedOrderInfo> threadFailedOrders = new CopyOnWriteArrayList<>();
        LongAdderAtomicReference maxCreateTimeRef = new LongAdderAtomicReference(null);
        LongAdderAtomicReference minFailedCreateTimeRef = new LongAdderAtomicReference(null);

        CountDownLatch latch = new CountDownLatch(partitions.size());

        for (List<OrderListRespDTO.OrderDetail> partition : partitions) {
            compensateExecutor.execute(() -> {
                try {
                    for (OrderListRespDTO.OrderDetail order : partition) {
                        try {
                            upsertOrder(order, platformStoreId, merchantCode, erpStoreCode);
                            totalSuccess.incrementAndGet();

                            if (order.getCreateTime() != null) {
                                maxCreateTimeRef.updateIfGreater(order.getCreateTime());
                            }
                        } catch (Exception e) {
                            log.error("【订单保存失败】orderId={}, error={}", order.getOrderId(), e.getMessage());
                            allFailedOrderIds.add(order.getOrderId());

                            if (order.getCreateTime() != null) {
                                minFailedCreateTimeRef.updateIfLesser(order.getCreateTime());
                            }

                            FailedOrderInfo failedInfo = new FailedOrderInfo();
                            failedInfo.setOrderId(order.getOrderId());
                            failedInfo.setChannelOrderId(order.getChannelOrderId());
                            failedInfo.setPlatformStoreId(platformStoreId);
                            failedInfo.setMerchantCode(merchantCode);
                            failedInfo.setErpStoreCode(erpStoreCode);
                            failedInfo.setErrorMessage(e.getMessage());
                            failedInfo.setFailTimestamp(System.currentTimeMillis());
                            failedInfo.setOrderDetail(order);
                            failedInfo.setRetryCount(0);
                            threadFailedOrders.add(failedInfo);

                            EleOrderRetryTaskSubmitter.RetryTask retryTask = new EleOrderRetryTaskSubmitter.RetryTask(
                                    order.getOrderId(), order.getChannelOrderId(),
                                    platformStoreId, merchantCode, erpStoreCode,
                                    null, order);
                            List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
                            tasks.add(retryTask);
                            retryTaskSubmitter.submitRetryTasks(tasks);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(10, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("【多线程保存超时】门店{}部分订单未完成", platformStoreId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【多线程保存中断】门店{}", platformStoreId);
        }

        failedOrders.addAll(threadFailedOrders);

        Long finalSyncTime = calculateSyncTime(maxCreateTimeRef.get(), minFailedCreateTimeRef.get(),
                totalSuccess.get(), orders.size());

        updateSyncLogWithFailedDetails(platformStoreId, merchantCode, erpStoreCode,
                orders.size(), totalSuccess.get(), new ArrayList<>(allFailedOrderIds), finalSyncTime, threadCount);

        return maxCreateTimeRef.get();
    }

    /**
     * 计算 syncTime 推进策略
     */
    private Long calculateSyncTime(Long maxCreateTime, Long minFailedCreateTime, int successCount, int totalCount) {
        double successRate = (double) successCount / totalCount;

        if (successRate == 1.0) {
            return maxCreateTime;
        } else if (successRate > 0.8 && minFailedCreateTime != null) {
            return minFailedCreateTime - 1;
        } else {
            log.warn("【syncTime】失败率过高({}%)，不推进syncTime", (1 - successRate) * 100);
            return null;
        }
    }

    /**
     * 原子操作的Long引用类
     */
    private static class LongAdderAtomicReference {
        private volatile Long value;

        public LongAdderAtomicReference(Long initialValue) {
            this.value = initialValue;
        }

        public void updateIfGreater(Long newValue) {
            synchronized (this) {
                if (newValue != null && (value == null || newValue > value)) {
                    value = newValue;
                }
            }
        }

        public void updateIfLesser(Long newValue) {
            synchronized (this) {
                if (newValue != null && (value == null || newValue < value)) {
                    value = newValue;
                }
            }
        }

        public Long get() {
            return value;
        }
    }

    /**
     * UPSERT单个订单（插入或更新）
     * 
     * 优势:
     * 1. 一次SQL完成插入或更新，不需要先查询
     * 2. 基于唯一索引保证幂等性
     * 3. 天然支持并发，不需要分布式锁
     */
    void upsertOrder(OrderListRespDTO.OrderDetail orderDetail, String platformStoreId,
            String merchantCode, String erpStoreCode) {
        String orderId = orderDetail.getOrderId();

        if (shutdownStateManager.isShuttingDown()) {
            log.warn("【订单同步】应用正在关闭，跳过订单处理，orderId={}", orderId);
            throw new EleOrderSyncException("【订单同步】应用正在关闭，跳过订单处理，orderId=" + orderId);
        }

        // 1. 构建订单DO
        OrderDO orderDO = buildOrderDO(orderDetail, erpStoreCode);

        // 2. UPSERT订单主表（INSERT ... ON DUPLICATE KEY UPDATE）
        orderMapper.upsertOrder(orderDO);

        // 3. UPSERT平台信息
        upsertPlatform(orderDetail);

        // 4. 保存子订单和优惠信息
        replaceItems(orderDetail, erpStoreCode);
        replaceDiscounts(orderDetail);
    }

    /**
     * 更新同步日志（包含失败详情）
     */
    private void updateSyncLogWithFailedDetails(String platformStoreId, String merchantCode, String erpStoreCode,
            int totalCount, int successCount, List<String> failedOrderIds,
            Long syncTime, Integer threadCount) {
        try {
            EleOrderSyncLog syncLog = eleOrderSyncLogMapper.selectByStoreId(platformStoreId);

            if (syncLog == null) {
                syncLog = new EleOrderSyncLog();
                syncLog.setPlatformStoreId(platformStoreId);
                syncLog.setMerchantCode(merchantCode);
                syncLog.setErpStoreCode(erpStoreCode);
                syncLog.setCreateTime(System.currentTimeMillis());
                syncLog.setSyncCount(totalCount);
                syncLog.setTotalPulled(totalCount);
                syncLog.setSuccessCount(successCount);
                syncLog.setFailCount(failedOrderIds.size());
                syncLog.setFailedOrderCount(failedOrderIds.size());
                syncLog.setSyncTime(syncTime);
                syncLog.setSyncEndTime(LocalDateTime.now());

                if (failedOrderIds.isEmpty()) {
                    syncLog.setStatus(1);
                    syncLog.setPartialFailed(0);
                    syncLog.setErrorMsg(null);
                } else if (successCount > 0) {
                    syncLog.setStatus(0);
                    syncLog.setPartialFailed(1);
                    syncLog.setFailedOrderIds(objectMapper.writeValueAsString(failedOrderIds));
                    syncLog.setErrorMsg("部分订单保存失败，共" + failedOrderIds.size() + "个");
                } else {
                    syncLog.setStatus(0);
                    syncLog.setPartialFailed(0);
                    syncLog.setErrorMsg("所有订单保存失败");
                }

                eleOrderSyncLogMapper.insert(syncLog);
            } else {
                syncLog.setSyncBatchId(syncLog.getSyncBatchId());
                syncLog.setSyncCount(totalCount);
                syncLog.setTotalPulled(totalCount);
                syncLog.setSuccessCount(successCount);
                syncLog.setFailCount(failedOrderIds.size());
                syncLog.setFailedOrderCount(failedOrderIds.size());
                syncLog.setSyncTime(syncTime);
                syncLog.setSyncEndTime(LocalDateTime.now());

                if (failedOrderIds.isEmpty()) {
                    syncLog.setStatus(1);
                    syncLog.setPartialFailed(0);
                    syncLog.setErrorMsg(null);
                } else if (successCount > 0) {
                    syncLog.setStatus(0);
                    syncLog.setPartialFailed(1);
                    syncLog.setFailedOrderIds(objectMapper.writeValueAsString(failedOrderIds));
                    syncLog.setErrorMsg("部分订单保存失败，共" + failedOrderIds.size() + "个");
                } else {
                    syncLog.setStatus(0);
                    syncLog.setPartialFailed(0);
                    syncLog.setErrorMsg("所有订单保存失败");
                }

                eleOrderSyncLogMapper.updateById(syncLog);
            }
        } catch (Exception e) {
            log.error("【更新同步日志失败】platformStoreId={}, error={}", platformStoreId, e.getMessage());
        }
    }

    /**
     * 批量插入新订单（废弃，保留以兼容旧代码）
     * 
     * @deprecated 使用 upsertOrder 方法替代
     */
    @Deprecated
    private void batchInsertNewOrders(List<OrderListRespDTO.OrderDetail> newOrders, String erpStoreCode) {
        // 保留方法签名，但实际不再使用
        log.warn("【废弃方法】batchInsertNewOrders 已被 upsertOrder 替代");
    }

    /**
     * 处理单个订单（废弃，保留以兼容旧代码）
     * 
     * @deprecated 使用 upsertOrder 方法替代
     */
    @Deprecated
    private void processSingleOrder(OrderListRespDTO.OrderDetail orderDetail,
            String platformStoreId, String merchantCode, String erpStoreCode, boolean overwrite) {
        // 保留方法签名，但实际不再使用
        log.warn("【废弃方法】processSingleOrder 已被 upsertOrder 替代");
        upsertOrder(orderDetail, platformStoreId, merchantCode, erpStoreCode);
    }

    @Override
    public void consumeOrderMessage(OrderMessage message) {
        consumeOrderMessage(message, false);
    }

    @Override
    public void consumeOrderMessage(OrderMessage message, boolean overwrite) {
        saveOrUpdateBatch(List.of(convertMessageToOrderDetail(message)), message.getPlatformStoreId(),
                message.getMerchantCode(), message.getErpStoreCode(), overwrite);

        Integer status = message.getStatus();
        if (status != null && status != -1 && status != 6) {
            eleOrderTrackingService.startTracking(
                    message.getOrderId(),
                    message.getPlatformStoreId(),
                    message.getMerchantCode(),
                    message.getErpStoreCode(),
                    message.getChannelOrderId(),
                    status,
                    message.getCreateTime() != null ? message.getCreateTime() : System.currentTimeMillis() / 1000);
        } else if (status != null) {
            eleOrderTrackingService.updateTracking(message.getOrderId(), status);
        }
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
        order.setStoreCode(StrUtil.trim(detail.getStoreCode()));
        order.setLongitude(detail.getLongitude());
        order.setLatitude(detail.getLatitude());
        order.setUserId(detail.getUserId());
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

    private void saveRemoteOrderDetailToLocal(OrderDetailRespDTO detail, String platformStoreId,
            String merchantCode, String erpStoreCode) {
        if (detail == null || detail.getOrderId() == null) {
            return;
        }
        if (!isTerminalStatus(detail.getStatus())) {
            log.info("【跳过详情落库】非目标终态订单，orderId={}, status={}", detail.getOrderId(), detail.getStatus());
            return;
        }

        OrderListRespDTO.OrderDetail orderDetail = convertToOrderListDetail(detail);
        upsertOrder(orderDetail, platformStoreId, merchantCode, erpStoreCode);
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

        for (EleOrderFailRecord record : pendingRecords) {
            record.setProcessStatus("RETRYING");
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
        }
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
            List<StorePlatformRespVO> stores = storeService.getAllPlatformStoresByPlatformCode(null);
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
        dto.setStoreCode(StrUtil.trim(data.getStore_code()));
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
                saveRemoteOrderDetailToLocal(dto, platformStoreId, finalMerchantCode, finalErpStoreCode);
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

    /**
     * 定时检测中间态订单
     * 
     * 检测规则:
     * 1. 失败记录表: status='FAILED' 且 create_time < 24小时前
     * 2. 同步日志表: partial_failed=1 且 create_time < 1小时前(未补偿)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void detectIntermediateOrders() {
        log.info("【中间态检测】开始检测中间态订单...");

        int fixedCount = 0;
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        Long oneDayAgoTimestamp = oneDayAgo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;

        // 检测1: 失败超过24小时的FAILED记录
        List<EleOrderFailRecord> failedRecords = eleOrderFailRecordMapper.selectList(
                new LambdaQueryWrapperX<EleOrderFailRecord>()
                        .eq(EleOrderFailRecord::getProcessStatus, "FAILED")
                        .lt(EleOrderFailRecord::getCreateTime, oneDayAgoTimestamp));

        for (EleOrderFailRecord record : failedRecords) {
            log.warn("【中间态检测】发现长期失败订单，orderId={}, 失败时间={}", record.getOrderId(), record.getCreateTime());
            try {
                retryFailedOrder(record);
                fixedCount++;
            } catch (Exception e) {
                log.error("【中间态修复】修复失败，orderId={}, error={}", record.getOrderId(), e.getMessage());
            }
        }

        // 检测2: 部分失败超过1小时的同步日志（未补偿）
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<EleOrderSyncLog> partialLogs = eleOrderSyncLogMapper.selectList(
                new LambdaQueryWrapperX<EleOrderSyncLog>()
                        .eq(EleOrderSyncLog::getPartialFailed, 1)
                        .lt(EleOrderSyncLog::getSyncEndTime, oneHourAgo));

        for (EleOrderSyncLog logEntry : partialLogs) {
            log.warn("【中间态检测】发现未补偿的部分失败同步，platformStoreId={}, 同步时间={}",
                    logEntry.getPlatformStoreId(), logEntry.getSyncEndTime());
            try {
                compensatePartialFailed(logEntry);
                fixedCount++;
            } catch (Exception e) {
                log.error("【中间态修复】补偿失败，platformStoreId={}, error={}",
                        logEntry.getPlatformStoreId(), e.getMessage());
            }
        }

        log.info("【中间态检测】检测完成，共修复{}个中间态订单", fixedCount);
    }

    /**
     * 重试长期失败的订单
     */
    private void retryFailedOrder(EleOrderFailRecord record) {
        try {
            // 更新状态为RETRYING
            record.setProcessStatus("RETRYING");
            record.setRetryCount(record.getRetryCount() + 1);
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);

            // 从API重新拉取订单详情并保存
            EleOrderRetryTaskSubmitter.RetryTask retryTask = new EleOrderRetryTaskSubmitter.RetryTask(
                    record.getOrderId(), record.getChannelOrderId(),
                    record.getPlatformStoreId(), record.getMerchantCode(), record.getErpStoreCode(),
                    record.getId(), null);
            List<EleOrderRetryTaskSubmitter.RetryTask> tasks = new ArrayList<>();
            tasks.add(retryTask);
            retryTaskSubmitter.submitRetryTasks(tasks);

            // 标记成功
            record.setProcessStatus("SUCCESS");
            eleOrderFailRecordMapper.updateById(record);
        } catch (Exception e) {
            record.setProcessStatus("FAILED");
            record.setFailMessage("重试失败: " + e.getMessage());
            eleOrderFailRecordMapper.updateById(record);
            throw e;
        }
    }

    /**
     * 补偿部分失败的同步
     */
    private void compensatePartialFailed(EleOrderSyncLog logEntry) {
        if (StrUtil.isBlank(logEntry.getFailedOrderIds())) {
            return;
        }

        try {
            List<String> failedOrderIds = objectMapper.readValue(
                    logEntry.getFailedOrderIds(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });

            log.info("【补偿任务】门店{}, 补偿{}个失败订单", logEntry.getPlatformStoreId(), failedOrderIds.size());

            // 逐个重试失败订单
            for (String orderId : failedOrderIds) {
                try {
                    EleOrderFailRecord record = eleOrderFailRecordMapper.selectOne(
                            new LambdaQueryWrapperX<EleOrderFailRecord>()
                                    .eq(EleOrderFailRecord::getOrderId, orderId)
                                    .orderByDesc(EleOrderFailRecord::getCreateTime)
                                    .last("LIMIT 1"));

                    if (record != null) {
                        retryFailedOrder(record);
                    }
                } catch (Exception e) {
                    log.error("【补偿任务】单个订单补偿失败，orderId={}", orderId);
                }
            }

            log.info("【补偿任务】门店{}补偿完成", logEntry.getPlatformStoreId());
        } catch (Exception e) {
            log.error("【补偿任务】解析失败订单列表失败，syncLogId={}", logEntry.getId());
        }
    }

    /**
     * 保存订单并跟踪失败订单（新增方法，替代原有saveOrUpdateBatch）
     */
    private Long saveOrUpdateBatchWithFailureTracking(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, List<FailedOrderInfo> failedOrders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }

        int totalCount = orders.size();
        int threadCount = calculateWriteThreadCount(totalCount);

        log.info("【订单保存】开始保存{}个订单（含所有状态），动态分配{}线程", totalCount, threadCount);

        if (threadCount <= 1 || totalCount <= 50) {
            return saveOrdersSingleThread(orders, platformStoreId, merchantCode, erpStoreCode, failedOrders);
        } else {
            return saveOrdersMultiThread(orders, platformStoreId, merchantCode, erpStoreCode, threadCount,
                    failedOrders);
        }
    }

    /**
     * 执行补偿任务（在订单拉取线程结束后调用）
     */
    private CompensationResult executeCompensationTasks(List<FailedOrderInfo> failedOrders, String platformStoreId,
            String merchantCode, String erpStoreCode) {
        if (failedOrders.isEmpty()) {
            log.info("【补偿任务】门店{}无失败订单，跳过补偿", platformStoreId);
            return CompensationResult.success();
        }

        log.info("【补偿任务】门店{}开始执行补偿，失败订单数={}", platformStoreId, failedOrders.size());

        CompensationResult result = new CompensationResult();
        result.setTotalCount(failedOrders.size());
        result.setAllCompleted(false);

        List<CompletableFuture<CompensationTaskResult>> futures = new ArrayList<>();

        for (FailedOrderInfo failedInfo : failedOrders) {
            CompletableFuture<CompensationTaskResult> future = CompletableFuture.supplyAsync(() -> {
                return compensateSingleOrder(failedInfo, platformStoreId, merchantCode, erpStoreCode);
            }, compensateExecutor);

            futures.add(future);
        }

        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            allFutures.get(5, TimeUnit.MINUTES);

            for (CompletableFuture<CompensationTaskResult> future : futures) {
                CompensationTaskResult taskResult = future.get();
                result.addResult(taskResult);
            }

            result.setAllCompleted(true);

        } catch (Exception e) {
            log.error("【补偿任务】门店{}补偿异常", platformStoreId, e);
            result.setAllCompleted(false);
        }

        log.info("【补偿任务】门店{}补偿完成，成功={}, 仍失败={}",
                platformStoreId, result.getSuccessCount(), result.getFailedCount());

        return result;
    }

    /**
     * 补偿单个订单
     */
    private CompensationTaskResult compensateSingleOrder(FailedOrderInfo failedInfo, String platformStoreId,
            String merchantCode, String erpStoreCode) {
        CompensationTaskResult taskResult = new CompensationTaskResult();
        taskResult.setOrderId(failedInfo.getOrderId());
        taskResult.setPlatformStoreId(platformStoreId);
        taskResult.setMerchantCode(merchantCode);
        taskResult.setErpStoreCode(erpStoreCode);

        try {
            OrderDetailRespDTO detail = getOrderDetailRemote(
                    platformStoreId, merchantCode, erpStoreCode, failedInfo.getOrderId());

            if (detail == null) {
                taskResult.setSuccess(false);
                taskResult.setErrorMessage("订单详情不存在");
                return taskResult;
            }

            OrderMessage message = buildMessage(
                    convertToOrderListDetail(detail),
                    platformStoreId, merchantCode, erpStoreCode);

            consumeOrderMessage(message);

            taskResult.setSuccess(true);
            taskResult.setErrorMessage(null);

            log.info("【补偿成功】orderId={}", failedInfo.getOrderId());

        } catch (Exception e) {
            taskResult.setSuccess(false);
            taskResult.setErrorMessage(e.getMessage());
            taskResult.setException(e);

            log.error("【补偿失败】orderId={}, error={}", failedInfo.getOrderId(), e.getMessage());
        }

        return taskResult;
    }

    /**
     * 处理补偿结果
     * - 成功的订单：不处理
     * - 仍失败的订单：写入失败日志表 + 立即告警
     */
    private void handleCompensationResults(CompensationResult result) {
        if (result == null || result.getFailedCount() == 0) {
            log.info("【补偿结果】所有订单补偿成功");
            return;
        }

        for (CompensationTaskResult taskResult : result.getFailedTasks()) {
            String orderId = taskResult.getOrderId();
            String errorMessage = taskResult.getErrorMessage();

            EleOrderFailRecord record = saveFailRecordReturn(
                    orderId,
                    null,
                    "SYNC",
                    "COMPENSATION_FAILED",
                    errorMessage,
                    null,
                    null,
                    0,
                    null,
                    "PENDING_MANUAL",
                    taskResult.getPlatformStoreId(),
                    taskResult.getMerchantCode(),
                    taskResult.getErpStoreCode());

            sendImmediateAlert(record, taskResult);

            log.error("【补偿失败告警】orderId={}, error={}, recordId={}",
                    orderId, errorMessage, record.getId());
        }
    }

    /**
     * 立即发送告警（只要补偿失败就告警，无阈值）
     */
    private void sendImmediateAlert(EleOrderFailRecord record, CompensationTaskResult taskResult) {
        try {
            String traceId = TracerUtils.getTraceId();

            String alertMessage = String.format(
                    "【订单同步告警】\n" +
                            "门店：%s\n" +
                            "订单：%s\n" +
                            "失败阶段：COMPENSATION_FAILED\n" +
                            "失败原因：%s\n" +
                            "记录ID：%d\n" +
                            "TraceId：%s\n" +
                            "处理建议：订单已重试3次仍失败，请人工介入处理",
                    record.getPlatformStoreId(),
                    record.getOrderId(),
                    record.getFailMessage(),
                    record.getId(),
                    StrUtil.isNotBlank(traceId) ? traceId : "无");

            log.error("【订单告警】{}", alertMessage);

        } catch (Exception e) {
            log.error("【发送告警失败】orderId={}, error={}", record.getOrderId(), e.getMessage());
        }
    }

    /**
     * 手动重试失败记录（使用retryExecutor异步执行）
     */
    private void retryFailRecordForSingleOrder(EleOrderFailRecord record, boolean overwrite) {
        record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
        record.setProcessStatus("RETRYING");
        record.setUpdateTime(System.currentTimeMillis());
        eleOrderFailRecordMapper.updateById(record);

        retryExecutor.execute(() -> {
            try {
                OrderDetailRespDTO detail = getOrderDetailRemote(
                        record.getPlatformStoreId(), record.getMerchantCode(), record.getErpStoreCode(),
                        record.getOrderId());
                consumeOrderMessage(buildMessage(convertToOrderListDetail(detail), record.getPlatformStoreId(),
                        record.getMerchantCode(), record.getErpStoreCode()), overwrite);
                record.setProcessStatus("SUCCESS");
                record.setUpdateTime(System.currentTimeMillis());
                eleOrderFailRecordMapper.updateById(record);
                log.info("【手动重试成功】orderId={}", record.getOrderId());
            } catch (Exception e) {
                record.setProcessStatus("FAILED");
                record.setFailMessage(e.getMessage().length() > 1000
                        ? e.getMessage().substring(0, 1000)
                        : e.getMessage());
                record.setUpdateTime(System.currentTimeMillis());
                eleOrderFailRecordMapper.updateById(record);
                log.error("【手动重试失败】orderId={}, error={}", record.getOrderId(), e.getMessage());
            }
        });
    }

    private long getTotalCountByStatus(String platformStoreId, String merchantCode, String erpStoreCode,
            Long startTime, Long endTime) {
        long totalCount = 0;
        Integer[] targetStatuses = { 1, 2, 3, 4, 5, 6, -1 };

        for (Integer status : targetStatuses) {
            try {
                OrderListReqDTO req = new OrderListReqDTO();
                req.setPlatformStoreId(platformStoreId);
                req.setMerchantCode(merchantCode);
                req.setErpStoreCode(erpStoreCode);
                req.setStartTime(startTime);
                req.setEndTime(endTime);
                req.setStatus(status);
                req.setPageSize(1);
                req.setScrollId(null);

                ORDER_LIST_RATE_LIMITER.acquire();
                OrderListRespDTO result = getOrderList(req);

                if (result != null && result.getTotal() != null) {
                    totalCount += result.getTotal();
                }
            } catch (Exception e) {
                log.warn("【动态窗口】获取状态{}的总数失败，platformStoreId={}", status, platformStoreId, e);
            }
        }

        return totalCount;
    }

    private int calculateWindowCount(long totalCount) {
        if (totalCount < 100) {
            return 1;
        }
        if (totalCount < 500) {
            return 2;
        }
        if (totalCount < 2000) {
            return Math.min(4, (int) Math.ceil((double) totalCount / 500));
        }
        if (totalCount < 5000) {
            return Math.min(6, (int) Math.ceil((double) totalCount / 800));
        }

        return Math.max(6, Math.min(10, (int) Math.ceil((double) totalCount / 1000)));
    }

    private List<OrderListRespDTO.OrderDetail> pullAllOrdersWithWindows(
            String platformStoreId, String merchantCode, String erpStoreCode,
            Long startTime, Long endTime, int windowCount) {

        List<TimeWindow> windows = splitTimeWindows(startTime, endTime, windowCount);
        log.info("【动态窗口】门店{}拆分{}个时间窗口", platformStoreId, windows.size());

        List<OrderListRespDTO.OrderDetail> allOrders = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successWindows = new AtomicInteger(0);
        AtomicInteger failWindows = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(windows.size());

        for (int i = 0; i < windows.size(); i++) {
            TimeWindow window = windows.get(i);
            final int windowIndex = i + 1;
            compensateExecutor.execute(() -> {
                boolean locked = false;
                try {
                    locked = eleOrderLockService.tryLockSyncWindow(
                            platformStoreId, window.getStart(), window.getEnd(),
                            0, windowLockLeaseMinutes);

                    if (!locked) {
                        log.warn("【动态窗口】窗口{}/{}获取锁失败，platformStoreId={}, window=[{}-{}]",
                                windowIndex, windows.size(), platformStoreId, window.getStart(), window.getEnd());
                        failWindows.incrementAndGet();
                        return;
                    }

                    log.info("【动态窗口】开始拉取窗口{}/{}，platformStoreId={}, 时间范围: {} ~ {}",
                            windowIndex, windows.size(), platformStoreId,
                            new Date(window.getStart() * 1000),
                            new Date(window.getEnd() * 1000));

                    List<OrderListRespDTO.OrderDetail> windowOrders = pullAllOrders(
                            platformStoreId, merchantCode, erpStoreCode,
                            window.getStart(), window.getEnd());

                    allOrders.addAll(windowOrders);
                    successWindows.incrementAndGet();

                    log.info("【动态窗口】窗口{}/{}完成，platformStoreId={}, 拉取{}条",
                            windowIndex, windows.size(), platformStoreId, windowOrders.size());
                } catch (Exception e) {
                    failWindows.incrementAndGet();
                    log.error("【动态窗口】窗口{}/{}拉取失败，platformStoreId={}",
                            windowIndex, windows.size(), platformStoreId, e);
                } finally {
                    if (locked) {
                        eleOrderLockService.unlockSyncWindow(
                                platformStoreId, window.getStart(), window.getEnd());
                    }
                    latch.countDown();
                }
            });
        }

        try {
            long timeoutMinutes = Math.max(5, (windowCount * 30L) / 60);
            boolean completed = latch.await(timeoutMinutes, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("【动态窗口】部分窗口超时，未完成窗口数: {}/{}", latch.getCount(), windows.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【动态窗口】等待被中断，platformStoreId={}", platformStoreId, e);
        }

        List<OrderListRespDTO.OrderDetail> deduplicatedOrders = allOrders.stream()
                .collect(Collectors.toMap(
                        OrderListRespDTO.OrderDetail::getOrderId,
                        order -> order,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .sorted((o1, o2) -> {
                    if (o1.getCreateTime() == null && o2.getCreateTime() == null)
                        return 0;
                    if (o1.getCreateTime() == null)
                        return 1;
                    if (o2.getCreateTime() == null)
                        return -1;
                    return Long.compare(o1.getCreateTime(), o2.getCreateTime());
                })
                .collect(Collectors.toList());

        log.info("【动态窗口】门店{}所有窗口完成，成功{}/{}个，失败{}个，去重后共{}条",
                platformStoreId, successWindows.get(), windows.size(), failWindows.get(), deduplicatedOrders.size());

        return deduplicatedOrders;
    }

    private List<TimeWindow> splitTimeWindows(long startTime, long endTime, int windowCount) {
        List<TimeWindow> windows = new ArrayList<>();
        long totalDuration = endTime - startTime;
        long windowDuration = totalDuration / windowCount;

        for (int i = 0; i < windowCount; i++) {
            long windowStart = startTime + (i * windowDuration);
            long windowEnd;

            if (i == windowCount - 1) {
                windowEnd = endTime;
            } else {
                windowEnd = windowStart + windowDuration;
            }

            windows.add(new TimeWindow(windowStart, windowEnd));
        }

        log.info("【动态窗口】拆分{}个窗口，时间范围: {} ~ {}", windowCount,
                new Date(startTime * 1000), new Date(endTime * 1000));
        for (int i = 0; i < windows.size(); i++) {
            TimeWindow w = windows.get(i);
            log.info("【动态窗口】窗口{}/{}: {} ~ {}", i + 1, windows.size(),
                    new Date(w.getStart() * 1000), new Date(w.getEnd() * 1000));
        }

        return windows;
    }

    @lombok.Data
    private static class TimeWindow {
        private final long start;
        private final long end;

        public TimeWindow(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
