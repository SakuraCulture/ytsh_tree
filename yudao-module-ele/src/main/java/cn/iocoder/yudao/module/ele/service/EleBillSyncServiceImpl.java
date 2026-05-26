package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.ele.controller.admin.vo.BillSyncFailLogVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderBillVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderBillDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.BillSyncFailLogDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.mysql.BillSyncFailLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderBillMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderMapper;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.BillListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.BillListRespDTO;
import cn.iocoder.yudao.module.ele.util.MoneyUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformInfoRespVO;
import cn.iocoder.yudao.module.business.service.store.StorePlatformCacheService;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EleBillSyncServiceImpl implements EleBillSyncService {

    private static final String BILL_SYNC_LOCK_PREFIX = "ele:bill:sync:";
    private static final DateTimeFormatter BILL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BILL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private EleOpenApiClient eleOpenApiClient;
    @Resource
    private StorePlatformCacheService storePlatformCacheService;
    @Resource
    private OrderBillMapper orderBillMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private BillSyncFailLogMapper billSyncFailLogMapper;
    @Resource
    private EleApiConfigMapper eleApiConfigMapper;
    @Resource
    private RedissonClient redissonClient;

    private final RateLimiter billApiRateLimiter = RateLimiter.create(18.0);

    @Override
    public void syncAllBillsByDate(String billDate) {
        log.info("【账单同步】开始同步日期: {}", billDate);
        syncBillsForStoresInRange(billDate, billDate);
    }

    @Override
    public void syncAllBillsByDateRange(String startDate, String endDate) {
        log.info("【账单同步】开始同步日期范围: {} ~ {}", startDate, endDate);
        syncBillsForStoresInRange(startDate, endDate);
    }

    private void syncBillsForStoresInRange(String startDate, String endDate) {
        EleApiConfig config = getApiConfig();
        if (config == null || StrUtil.isBlank(config.getMerchantCode())) {
            log.error("【账单同步】未找到有效的API配置或merchantCode为空");
            return;
        }
        String merchantCode = config.getMerchantCode();
        log.info("【账单同步】从 ele_api_config 表获取 merchantCode: {}", merchantCode);

        List<StorePlatformInfoRespVO> stores = storePlatformCacheService.getStorePlatformListFromRedis();
        if (CollUtil.isEmpty(stores)) {
            log.warn("【账单同步】Redis中没有门店数据，请确保已同步门店信息到Redis");
            return;
        }
        log.info("【账单同步】从 Redis 获取到 {} 个门店", stores.size());

        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        int successCount = 0;
        int failCount = 0;
        int totalStoreDays = stores.size() * totalDays;
        int processedStoreDays = 0;

        for (StorePlatformInfoRespVO store : stores) {
            String erpStoreCode = StrUtil.trim(store.getPlatformStoreId());

            if (StrUtil.isBlank(erpStoreCode)) {
                log.warn("【账单同步】门店{}缺少platformStoreId，跳过", store.getStoreName());
                failCount += totalDays;
                processedStoreDays += totalDays;
                continue;
            }

            LocalDate currentDate = start;
            while (!currentDate.isAfter(end)) {
                String billDate = currentDate.toString();
                processedStoreDays++;

                try {
                    log.info("【账单同步】门店 {}/{} 日期 {}/{}: {}",
                            store.getStoreName(),
                            (stores.indexOf(store) + 1), stores.size(),
                            processedStoreDays, totalStoreDays,
                            billDate);
                    syncBillByStoreAndDate(merchantCode, erpStoreCode, billDate);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("【账单同步】门店{} 日期{} 失败: {}",
                            store.getStoreName(), billDate, e.getMessage(), e);
                    saveFailLog(merchantCode, erpStoreCode, store.getStoreName(),
                            billDate, 1, e.getMessage());
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        log.info("【账单同步】完成，成功{}家次，失败{}家次", successCount, failCount);
    }

    @Override
    public void syncBillByStoreAndDate(String merchantCode, String platformStoreId, String billDate) {
        String lockKey = BILL_SYNC_LOCK_PREFIX + platformStoreId + ":" + billDate;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.info("【账单同步-去重】门店账单正在同步，跳过，storeCode={}, billDate={}", platformStoreId, billDate);
                return;
            }

            int pageNum = 1;
            int pageSize = 100;
            Long total = null;
            int syncedCount = 0;
            int maxRetry = 3;

            while (true) {
                BillListReqDTO req = new BillListReqDTO();
                req.setMerchantCode(merchantCode);
                req.setErpStoreCode(platformStoreId);
                req.setBillDate(billDate);
                                req.setPageNum(pageNum);
                req.setPageSize(pageSize);

                BillListRespDTO resp = fetchBillPageWithRetry(req, platformStoreId, pageNum, maxRetry);
                if (resp == null || CollUtil.isEmpty(resp.getBillDetails())) {
                    break;
                }

                if (total == null) {
                    total = resp.getTotal();
                    if (total == null || total == 0) {
                        log.info("【账单同步】门店{} 日期{} 无数据", platformStoreId, billDate);
                        break;
                    }
                    log.info("【账单同步】门店{} 日期{} total={}条", platformStoreId, billDate, total);
                }

                saveBillBatch(resp.getBillDetails(), billDate);
                updateOrderSettlement(resp.getBillDetails());
                syncedCount += resp.getBillDetails().size();
                pageNum++;

                if (total != null && (long) (pageNum - 1) * pageSize >= total) {
                    break;
                }

                sleep(50);
            }

            log.info("【账单同步】门店{} 日期{} 完成，同步{}条", platformStoreId, billDate, syncedCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【账单同步】获取锁被中断，storeCode={}, billDate={}", platformStoreId, billDate);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void retryFailedBillSync() {
        log.info("【账单同步】开始重试失败的账单同步");
        List<BillSyncFailLogDO> failLogs = billSyncFailLogMapper.selectPendingList(200);
        for (BillSyncFailLogDO failLog : failLogs) {
            try {
                retryFailedBillSyncByLogId(failLog.getId());
            } catch (Exception e) {
                log.error("【账单同步】重试失败日志ID={} 失败: {}", failLog.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void retryFailedBillSyncByLogId(Long logId) {
        BillSyncFailLogDO failLog = billSyncFailLogMapper.selectById(logId);
        if (failLog == null) {
            log.warn("【账单同步】失败日志ID={} 不存在", logId);
            return;
        }
        billSyncFailLogMapper.markRetrying(logId, System.currentTimeMillis());
        try {
            syncBillByStoreAndDate(failLog.getMerchantCode(), failLog.getStoreCode(), failLog.getBillDate().toString());
            billSyncFailLogMapper.markResolved(logId, System.currentTimeMillis());
        } catch (Exception e) {
            billSyncFailLogMapper.updateRetryCount(logId, failLog.getRetryCount() + 1, e.getMessage());
            throw e;
        }
    }

    @Override
    public void retryByOrderId(String orderId) {
        OrderBillDO billDO = orderBillMapper.selectLatestByOrderId(orderId);
        if (billDO == null) {
            log.warn("【账单重试】未找到订单{}的账单信息", orderId);
            return;
        }
        String storeCode = billDO.getStoreCode();
        String merchantCode = billDO.getMerchantCode();
        String billDate = billDO.getBillDate() != null ? billDO.getBillDate().toString() : null;
        if (StrUtil.isBlank(storeCode) || StrUtil.isBlank(merchantCode) || StrUtil.isBlank(billDate)) {
            log.warn("【账单重试】订单{}缺少必要信息", orderId);
            return;
        }
        log.info("【账单重试】订单{} 重试门店{} 日期{}", orderId, storeCode, billDate);
        syncBillByStoreAndDate(merchantCode, storeCode, billDate);
    }

    @Override
    public OrderBillVO getOrderBillInfo(String orderId) {
        log.info("【账单详情】查询订单账单，传入参数 orderId={}", orderId);
        if (orderId == null || orderId.isEmpty()) {
            log.warn("【账单详情】订单号为空");
            OrderBillVO vo = new OrderBillVO();
            vo.setOrderId(orderId);
            vo.setBillDetails(Collections.emptyList());
            vo.setTotalStatus(0);
            vo.setTotalBillAmount(BigDecimal.ZERO);
            return vo;
        }
        List<OrderBillDO> bills = orderBillMapper.selectByOrderId(orderId);
        log.info("【账单详情】查询结果，orderId={}, 账单数={}, deleted=0", orderId, bills == null ? 0 : bills.size());
        if (CollUtil.isEmpty(bills)) {
            OrderBillVO vo = new OrderBillVO();
            vo.setOrderId(orderId);
            vo.setBillDetails(Collections.emptyList());
            vo.setTotalStatus(0);
            vo.setTotalBillAmount(BigDecimal.ZERO);
            return vo;
        }

        OrderBillVO vo = new OrderBillVO();
        vo.setOrderId(orderId);
        List<OrderBillVO.BillDetailVO> details = new ArrayList<>();
        long totalBillAmount = 0;

        for (OrderBillDO billDO : bills) {
            OrderBillVO.BillDetailVO detail = new OrderBillVO.BillDetailVO();
            detail.setBillId(billDO.getBillId());
            detail.setBillDate(billDO.getBillDate() != null ? billDO.getBillDate().toString() : null);
            detail.setOrderDate(billDO.getOrderDate() != null ? billDO.getOrderDate().toString() : null);
            detail.setStatus(billDO.getStatus());
            detail.setStatusText(billDO.getStatus() != null && billDO.getStatus() == 1 ? "已结算" : "未结算");
            detail.setBillAmount(MoneyUtils.fenToYuan(billDO.getBillAmount()));
            detail.setItemPrice(MoneyUtils.fenToYuan(billDO.getItemPrice()));
            detail.setPackageFee(MoneyUtils.fenToYuan(billDO.getPackageFee()));
            detail.setDeliveryFee(MoneyUtils.fenToYuan(billDO.getDeliveryFee()));
            detail.setShopMarketingFee(MoneyUtils.fenToYuan(billDO.getShopMarketingFee()));
            detail.setPlatformFee(MoneyUtils.fenToYuan(billDO.getPlatformFee()));
            detail.setDonationFee(MoneyUtils.fenToYuan(billDO.getDonationFee()));
            detail.setUserPayShippingAmount(MoneyUtils.fenToYuan(billDO.getUserPayShippingAmount()));
            detail.setUserOnlinePayAmount(MoneyUtils.fenToYuan(billDO.getUserOnlinePayAmount()));
            detail.setProductPreferences(MoneyUtils.fenToYuan(billDO.getProductPreferences()));
            detail.setNotProductPreferences(MoneyUtils.fenToYuan(billDO.getNotProductPreferences()));
            detail.setPerformanceServiceFee(MoneyUtils.fenToYuan(billDO.getPerformanceServiceFee()));
            detail.setPlatformChargeFee(MoneyUtils.fenToYuan(billDO.getPlatformChargeFee()));
            detail.setActivityAmount(MoneyUtils.fenToYuan(billDO.getActivityAmount()));
            detail.setBillTypeDesc(billDO.getBillTypeDesc());
            detail.setShippingType(billDO.getShippingType());
            detail.setSettleOrderId(billDO.getSettleOrderId());
            detail.setRefundId(billDO.getRefundId());
            detail.setShopId(billDO.getShopId());
            detail.setCreateTime(billDO.getCreateTime() != null ? formatTimestamp(billDO.getCreateTime()) : null);
            detail.setUpdateTime(billDO.getUpdateTime() != null ? formatTimestamp(billDO.getUpdateTime()) : null);
            details.add(detail);

                        if ("正向单".equals(billDO.getBillTypeDesc()) || "代运营业务".equals(billDO.getBillTypeDesc())) {
                if (billDO.getBillAmount() != null) {
                    totalBillAmount += billDO.getBillAmount();
                }
            }
        }

        vo.setBillDetails(details);
        vo.setTotalBillAmount(MoneyUtils.fenToYuan(totalBillAmount));
        vo.setTotalStatus(bills.stream().allMatch(b -> b.getStatus() != null && b.getStatus() == 1) ? 1 : 0);
        return vo;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return null;
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public OrderBillVO getBillSummary(String orderId) {
        List<OrderBillDO> bills = orderBillMapper.selectByOrderId(orderId);
        if (CollUtil.isEmpty(bills)) {
            OrderBillVO vo = new OrderBillVO();
            vo.setOrderId(orderId);
            vo.setBillDetails(Collections.emptyList());
            vo.setTotalStatus(0);
            vo.setTotalBillAmount(BigDecimal.ZERO);
            return vo;
        }

        long positiveAmount = bills.stream()
                .filter(b -> "正向单".equals(b.getBillTypeDesc()))
                .mapToLong(OrderBillDO::getBillAmount)
                .sum();

        long operationAmount = bills.stream()
                .filter(b -> "代运营业务".equals(b.getBillTypeDesc()))
                .mapToLong(OrderBillDO::getBillAmount)
                .sum();

        long totalSettlement = positiveAmount + operationAmount;

        OrderBillVO vo = new OrderBillVO();
        vo.setOrderId(orderId);
        vo.setTotalBillAmount(MoneyUtils.fenToYuan(totalSettlement));
        vo.setTotalStatus(bills.stream().allMatch(b -> b.getStatus() != null && b.getStatus() == 1) ? 1 : 0);
        vo.setBillDetails(Collections.emptyList());
        return vo;
    }

    @Override
    public List<BillSyncFailLogVO> getFailLogList() {
        return billSyncFailLogMapper.selectList(200).stream()
                .map(this::convertFailLogToVO)
                .collect(Collectors.toList());
    }

    private BillListRespDTO fetchBillPageWithRetry(BillListReqDTO req, String platformStoreId, int pageNum, int maxRetry) {
        billApiRateLimiter.acquire();
        for (int retry = 0; retry <= maxRetry; retry++) {
            try {
                return eleOpenApiClient.getBillList(req);
            } catch (Exception e) {
                if (retry < maxRetry) {
                    long backoffMs = 200 * (1L << retry);
                    log.warn("【账单同步】门店{} 第{}页请求失败，{}ms后重试({}/{}): {}",
                            platformStoreId, pageNum, backoffMs, retry + 1, maxRetry, e.getMessage());
                    sleep(backoffMs);
                } else {
                    log.error("【账单同步】门店{} 第{}页请求失败，已达最大重试次数", platformStoreId, pageNum);
                    saveFailLog(req.getMerchantCode(), platformStoreId, null, req.getBillDate(), pageNum, e.getMessage());
                    throw e;
                }
            }
        }
        return null;
    }

    private void saveBillBatch(List<BillListRespDTO.BillDetailDTO> bills, String billDate) {
        if (CollUtil.isEmpty(bills)) {
            return;
        }
        String tenantId = TenantContextHolder.getTenantId() != null ? TenantContextHolder.getTenantId().toString() : "1";
        LocalDate requestBillDate = LocalDate.parse(billDate, BILL_DATE_FORMATTER);
        for (BillListRespDTO.BillDetailDTO bill : bills) {
            LocalDate sourceBillDate = parseBillDate(bill.getBillDate());
            if (sourceBillDate == null) {
                sourceBillDate = requestBillDate;
            }
            Long sourceCreateTime = parseBillDateTimeMillis(bill.getCreateTime());
            Long sourceUpdateTime = parseBillDateTimeMillis(bill.getUpdateTime());
            if (!isValidBillRow(bill, sourceBillDate, sourceCreateTime, sourceUpdateTime, requestBillDate)) {
                continue;
            }

            Date now = new Date();
            OrderBillDO billDO = new OrderBillDO();
            String settleId = StrUtil.isNotBlank(bill.getSettleOrderId()) ? bill.getSettleOrderId() : bill.getOrderId();
            String billType = bill.getBillTypeDesc();
            billDO.setBillId(settleId + "_" + sourceBillDate + "_" + billType);
            billDO.setOrderId(bill.getOrderId());
            LocalDate orderDate = parseBillDate(bill.getOrderDate());
            if (orderDate == null && StrUtil.isNotBlank(bill.getOrderDate())) {
                log.warn("【账单同步】orderDate格式非法，orderId={}, storeCode={}, requestBillDate={}, rawOrderDate={}",
                        bill.getOrderId(), bill.getStoreCode(), requestBillDate, bill.getOrderDate());
            }
            billDO.setOrderDate(orderDate);
            billDO.setRefundId(bill.getRefundId());
            billDO.setMerchantCode(bill.getMerchantCode());
            billDO.setStoreCode(bill.getStoreCode());
            billDO.setShopId(bill.getShopId());
            billDO.setStoreName(bill.getStoreName());
            billDO.setChannelType(bill.getChannelType());
            billDO.setBillDate(sourceBillDate);
            billDO.setRequestBillDate(requestBillDate);
            billDO.setStatus(bill.getStatus());
            billDO.setBillAmount(bill.getBillAmount());
            billDO.setItemPrice(bill.getItemPrice());
            billDO.setPackageFee(bill.getPackageFee());
            billDO.setDeliveryFee(bill.getDeliveryFee());
            billDO.setShopMarketingFee(bill.getShopMarketingFee());
            billDO.setPlatformFee(bill.getPlatformFee());
            billDO.setDonationFee(bill.getDonationFee());
            billDO.setUserPayShippingAmount(bill.getUserPayShippingAmount());
            billDO.setUserOnlinePayAmount(bill.getUserOnlinePayAmount());
            billDO.setProductPreferences(bill.getProductPreferences());
            billDO.setNotProductPreferences(bill.getNotProductPreferences());
            billDO.setPerformanceServiceFee(bill.getPerformanceServiceFee());
            billDO.setPlatformChargeFee(bill.getPlatformChargeFee());
            billDO.setActivityAmount(parseActivityAmount(bill, requestBillDate));
            billDO.setBillTypeDesc(billType);
            billDO.setShippingType(bill.getShippingType());
            billDO.setSettleOrderId(bill.getSettleOrderId());
            billDO.setSyncTime(now);
            billDO.setDbCreateTime(now);
            billDO.setDbUpdateTime(now);
            billDO.setCreateTime(sourceCreateTime);
            billDO.setUpdateTime(sourceUpdateTime);
            billDO.setTenantId(tenantId);
            billDO.setDeleted(false);
            orderBillMapper.rawInsertOrUpdate(billDO);
        }
    }

    private boolean isValidBillRow(BillListRespDTO.BillDetailDTO bill, LocalDate sourceBillDate,
                                   Long sourceCreateTime, Long sourceUpdateTime, LocalDate requestBillDate) {
        if (StrUtil.isBlank(bill.getOrderId())) {
            log.error("【账单同步】跳过账单，orderId为空，storeCode={}, requestBillDate={}", bill.getStoreCode(), requestBillDate);
            return false;
        }
        if (StrUtil.isBlank(bill.getMerchantCode())) {
            log.error("【账单同步】跳过账单，merchantCode为空，orderId={}, storeCode={}, requestBillDate={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate);
            return false;
        }
        if (StrUtil.isBlank(bill.getStoreCode())) {
            log.error("【账单同步】跳过账单，storeCode为空，orderId={}, requestBillDate={}", bill.getOrderId(), requestBillDate);
            return false;
        }
        if (sourceBillDate == null) {
            log.error("【账单同步】跳过账单，billDate为空或格式非法，orderId={}, storeCode={}, requestBillDate={}, sourceBillDate={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate, bill.getBillDate());
            return false;
        }
        if (sourceCreateTime == null) {
            log.error("【账单同步】跳过账单，createTime为空或格式非法，orderId={}, storeCode={}, requestBillDate={}, sourceCreateTime={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate, bill.getCreateTime());
            return false;
        }
        if (sourceUpdateTime == null) {
            log.error("【账单同步】跳过账单，updateTime为空或格式非法，orderId={}, storeCode={}, requestBillDate={}, sourceUpdateTime={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate, bill.getUpdateTime());
            return false;
        }
        if (bill.getStatus() == null) {
            log.error("【账单同步】跳过账单，status为空，orderId={}, storeCode={}, requestBillDate={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate);
            return false;
        }
        if (bill.getBillAmount() == null) {
            log.error("【账单同步】跳过账单，billAmount为空，orderId={}, storeCode={}, requestBillDate={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate);
            return false;
        }
        if (StrUtil.isBlank(bill.getBillTypeDesc())) {
            log.error("【账单同步】跳过账单，billTypeDesc为空，orderId={}, storeCode={}, requestBillDate={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate);
            return false;
        }
        return true;
    }

    private Long parseActivityAmount(BillListRespDTO.BillDetailDTO bill, LocalDate requestBillDate) {
        if (StrUtil.isBlank(bill.getActivityAmount())) {
            return null;
        }
        Long activityAmount = MoneyUtils.parseStringToLong(bill.getActivityAmount());
        if (activityAmount == null) {
            log.warn("【账单同步】activityAmount转换失败，orderId={}, storeCode={}, requestBillDate={}, rawValue={}",
                    bill.getOrderId(), bill.getStoreCode(), requestBillDate, bill.getActivityAmount());
        }
        return activityAmount;
    }

    private LocalDate parseBillDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), BILL_DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseBillDateTimeMillis(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value.trim(), BILL_DATETIME_FORMATTER);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    private void updateOrderSettlement(List<BillListRespDTO.BillDetailDTO> bills) {
        if (CollUtil.isEmpty(bills)) {
            return;
        }
        for (BillListRespDTO.BillDetailDTO bill : bills) {
            try {
                Long positiveAmount = orderBillMapper.sumPositiveAmountByOrderId(bill.getOrderId());
                Long operationAmount = orderBillMapper.sumOperationAmountByOrderId(bill.getOrderId());
                List<OrderBillDO> allBills = orderBillMapper.selectByOrderId(bill.getOrderId());

                long totalSettlement = (positiveAmount != null ? positiveAmount : 0L) +
                        (operationAmount != null ? operationAmount : 0L);

                int settlementStatus = allBills.stream()
                        .filter(b -> "正向单".equals(b.getBillTypeDesc())
                                || "代运营业务".equals(b.getBillTypeDesc()))
                        .allMatch(b -> b.getStatus() != null && b.getStatus() == 1) ? 1 : 0;

                LocalDate latestBillDate = allBills.stream()
                        .map(OrderBillDO::getBillDate)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(null);

                orderMapper.updateSettlementWithBillInfo(
                        bill.getOrderId(),
                        MoneyUtils.fenToYuan(totalSettlement),
                        settlementStatus,
                        latestBillDate,
                        bill.getBillAmount(),
                        bill.getStatus(),
                        "bill_sync",
                        System.currentTimeMillis()
                );
            } catch (Exception e) {
                log.warn("【账单同步】更新订单结算信息失败，orderId={}: {}", bill.getOrderId(), e.getMessage());
            }
        }
    }

    private String resolveMerchantCode(String settlementAccount) {
        if (StrUtil.isNotBlank(settlementAccount)) {
            return StrUtil.trim(settlementAccount);
        }
        EleApiConfig config = getApiConfig();
        return config != null ? StrUtil.trim(config.getMerchantCode()) : null;
    }

    private EleApiConfig getApiConfig() {
        try {
            return eleApiConfigMapper.selectActive();
        } catch (Exception e) {
            log.error("【账单同步】获取API配置失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveFailLog(String merchantCode, String storeCode, String storeName,
                             String billDate, int failPage, String reason) {
        try {
            BillSyncFailLogDO logDO = new BillSyncFailLogDO();
            logDO.setBillDate(LocalDate.parse(billDate));
            logDO.setMerchantCode(merchantCode);
            logDO.setStoreCode(storeCode);
            logDO.setStoreName(storeName);
            logDO.setFailPage(failPage);
            logDO.setFailReason(StrUtil.sub(reason, 0, 2000));
            logDO.setRetryCount(0);
            logDO.setRetryStatus(0);
            logDO.setSyncTime(new Date());
            logDO.setCreateTime(System.currentTimeMillis());
            logDO.setUpdateTime(System.currentTimeMillis());
            logDO.setTenantId(TenantContextHolder.getTenantId() != null ? TenantContextHolder.getTenantId().toString() : "1");
            logDO.setDeleted(false);
            billSyncFailLogMapper.rawInsertOrUpdate(logDO);
        } catch (Exception e) {
            log.error("【失败日志】写入失败: {}", e.getMessage());
        }
    }

    private BillSyncFailLogVO convertFailLogToVO(BillSyncFailLogDO logDO) {
        BillSyncFailLogVO vo = new BillSyncFailLogVO();
        vo.setId(logDO.getId());
        vo.setBillDate(logDO.getBillDate() != null ? logDO.getBillDate().toString() : "--");
        vo.setMerchantCode(logDO.getMerchantCode());
        vo.setStoreCode(logDO.getStoreCode());
        vo.setStoreName(logDO.getStoreName());
        vo.setFailPage(logDO.getFailPage());
        vo.setFailReason(logDO.getFailReason());
        vo.setRetryCount(logDO.getRetryCount());
        vo.setRetryStatus(logDO.getRetryStatus());
        vo.setRetryStatusText(getRetryStatusText(logDO.getRetryStatus()));
        vo.setLastRetryTime(logDO.getLastRetryTime() != null ? logDO.getLastRetryTime().toString() : "--");
        vo.setSyncTime(logDO.getSyncTime() != null ? logDO.getSyncTime().toString() : "--");
        return vo;
    }

    private String getRetryStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待重试";
            case 1 -> "重试中";
            case 2 -> "已解决";
            default -> "未知";
        };
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
