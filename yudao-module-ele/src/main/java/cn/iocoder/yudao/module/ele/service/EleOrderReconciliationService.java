package cn.iocoder.yudao.module.ele.service;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderSyncLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderMapper;
import cn.iocoder.yudao.module.ele.dal.redis.OrderSyncProgressCache;
import cn.iocoder.yudao.module.ele.enums.SyncErrorCode;
import cn.iocoder.yudao.module.ele.service.dto.StoreSyncProgress;
import cn.iocoder.yudao.module.ele.service.dto.SyncErrorDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单同步对账服务
 * 负责对比API拉取数量与落库数量，触发二次拉取，记录对账结果
 */
@Slf4j
@Service
public class EleOrderReconciliationService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;

    @Resource
    private OrderSyncProgressCache progressCache;

    /**
     * 对账结果 - 方案v1.2：三组数据对比（API原始总数、过滤后拉取数、实际落库数）
     */
    @Data
    public static class ReconciliationResult {
        private boolean consistent;
        private long apiRawTotal;        // API原始总数（包含所有状态）
        private long pulledTotal;        // 过滤后拉取数（7个目标状态）
        private long savedTotal;         // 实际落库总数
        private long pullDiscrepancy;    // 拉取差异（API总数 vs 拉取数）
        private long saveDiscrepancy;    // 保存差异（拉取数 vs 落库数）
        private double discrepancyRate;  // 差异率
        private int dataIntegrity;       // 1完整 2不完整
        private Map<Integer, Long> apiStatusCounts;
        private Map<Integer, Long> savedStatusCounts;
        private Map<Integer, Integer> pageCounts;
        private List<SyncErrorDTO> reconciliationErrors;
        private String reconciliationDetailJson;
        private int retryCount;

        public long getExpectedTotal() {
            return apiRawTotal;
        }

        public long getActualTotal() {
            return pulledTotal;
        }
    }

    /**
     * 执行对账 - 方案v1.2：三组数据对比
     *
     * @param platformStoreId 门店ID
     * @param apiRawTotal     API原始总数（包含所有状态）
     * @param pulledTotal     全状态拉取订单数（按 orderId 去重后）
     * @param savedTotal      实际落库总数
     * @param batchId         同步批次ID
     * @param syncLog         同步日志对象
     * @return 对账结果
     */
    public ReconciliationResult reconcile(String platformStoreId, long apiRawTotal,
                                           long pulledTotal,
                                           long savedTotal,
                                           String batchId,
                                           EleOrderSyncLog syncLog) {
        return reconcile(platformStoreId, apiRawTotal, null, pulledTotal, savedTotal, batchId, syncLog);
    }

    /**
     * 执行对账 - 兼容旧接口
     */
    public ReconciliationResult reconcile(String platformStoreId, long apiRawTotal,
                                           Map<Integer, Long> apiStatusCounts,
                                           long pulledTotal,
                                           long savedTotal,
                                           String batchId,
                                           EleOrderSyncLog syncLog) {
        ReconciliationResult result = new ReconciliationResult();
        result.setApiRawTotal(apiRawTotal);
        result.setPulledTotal(pulledTotal);
        result.setSavedTotal(savedTotal);
        result.setApiStatusCounts(new HashMap<>());

        long pullDiscrepancy = Math.abs(apiRawTotal - pulledTotal);
        long saveDiscrepancy = Math.abs(pulledTotal - savedTotal);
        result.setPullDiscrepancy(pullDiscrepancy);
        result.setSaveDiscrepancy(saveDiscrepancy);

        long totalDiscrepancy = Math.max(pullDiscrepancy, saveDiscrepancy);

        double discrepancyRate = 0.0;
        if (apiRawTotal > 0) {
            discrepancyRate = BigDecimal.valueOf(totalDiscrepancy)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(apiRawTotal), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        result.setDiscrepancyRate(discrepancyRate);
        result.setConsistent(totalDiscrepancy == 0);

        if (totalDiscrepancy == 0) {
            result.setDataIntegrity(1);
        } else {
            result.setDataIntegrity(2);
        }

        if (!result.isConsistent()) {
            SyncErrorDTO error = SyncErrorDTO.create(
                    SyncErrorCode.RECON_DISCREPANCY.getCode(),
                    SyncErrorCode.RECON_DISCREPANCY.getDesc(),
                    null,
                    String.format("API原始:%d, 拉取:%d(差异%d条), 落库:%d(差异%d条)",
                            apiRawTotal, pulledTotal, pullDiscrepancy, savedTotal, saveDiscrepancy),
                    true
            );
            result.setReconciliationErrors(new ArrayList<>());
            result.getReconciliationErrors().add(error);
        } else {
            result.setReconciliationErrors(new ArrayList<>());
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("apiRawTotal", apiRawTotal);
        detail.put("pulledTotal", pulledTotal);
        detail.put("savedTotal", savedTotal);
        detail.put("pullDiscrepancy", pullDiscrepancy);
        detail.put("saveDiscrepancy", saveDiscrepancy);
        detail.put("discrepancyRate", discrepancyRate);
        detail.put("integrityLevel", result.getDataIntegrity());
        result.setReconciliationDetailJson(JSONUtil.toJsonStr(detail));

        log.info("【对账完成】门店={}, API原始={}, 拉取={}, 落库={}, 拉取差异={}, 落库差异={}, 差异率={}%, 一致={}",
                platformStoreId, apiRawTotal, pulledTotal, savedTotal, pullDiscrepancy, saveDiscrepancy,
                discrepancyRate, result.isConsistent());

        return result;
    }

    /**
     * 更新同步日志的对账字段 - 方案v1.2：记录三组对比数据
     */
    public void updateSyncLogWithReconciliation(EleOrderSyncLog syncLog, ReconciliationResult result) {
        syncLog.setExpectedTotal((int) result.getApiRawTotal());
        syncLog.setActualTotal((int) result.getPulledTotal());
        syncLog.setSavedTotal((int) result.getSavedTotal());
        syncLog.setDiscrepancyRate(BigDecimal.valueOf(result.getDiscrepancyRate()));
        syncLog.setDataIntegrity(result.getDataIntegrity());
        if (result.getRetryCount() > 0) {
            syncLog.setRetryCount(result.getRetryCount());
        }

        syncLog.setReconciliationErrorCode(
                result.isConsistent() ? null : SyncErrorCode.RECON_DISCREPANCY.getCode()
        );
        syncLog.setReconciliationErrorDetail(
                result.isConsistent() ? null : result.getReconciliationDetailJson()
        );

        syncLog.setApiStatusCounts(null);
        syncLog.setSavedStatusCounts(null);
        syncLog.setPageCounts(null);
    }

    /**
     * 更新Redis进度中的对账数据 - 方案v1.2：使用三组对比数据
     */
    public void updateRedisProgress(String batchId, String platformStoreId,
                                     StoreSyncProgress progress, ReconciliationResult result) {
        if (progress == null) {
            progress = new StoreSyncProgress();
            progress.setBatchId(batchId);
            progress.setPlatformStoreId(platformStoreId);
        }

        progress.setTotalApiCount(result.getApiRawTotal());
        progress.setTotalSavedCount(result.getSavedTotal());
        progress.setDiscrepancyRate(result.getDiscrepancyRate());
        progress.setReconciliationStatus(result.isConsistent() ? "MATCH" : "MISMATCH");
        progress.setReconciliationErrors(result.getReconciliationErrors());

        progressCache.updateStoreProgress(batchId, platformStoreId, progress);
    }

    /**
     * 标记门店同步完成并清理Redis
     */
    public void markStoreCompleted(String batchId, String platformStoreId) {
        progressCache.markStoreCompleted(batchId, platformStoreId);
    }
}
