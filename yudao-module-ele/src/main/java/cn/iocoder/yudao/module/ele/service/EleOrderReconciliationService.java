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


@Slf4j
@Service
public class EleOrderReconciliationService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;

    @Resource
    private OrderSyncProgressCache progressCache;

    
    @Data
    public static class ReconciliationResult {
        private boolean consistent;
        private long apiRawTotal;                private long pulledTotal;                private long savedTotal;                 private long pullDiscrepancy;            private long saveDiscrepancy;            private double discrepancyRate;          private int dataIntegrity;               private Map<Integer, Long> apiStatusCounts;
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

    
    public ReconciliationResult reconcile(String platformStoreId, long apiRawTotal,
                                           long pulledTotal,
                                           long savedTotal,
                                           String batchId,
                                           EleOrderSyncLog syncLog) {
        return reconcile(platformStoreId, apiRawTotal, null, pulledTotal, savedTotal, batchId, syncLog);
    }

    
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


        return result;
    }

    
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

    
    public void markStoreCompleted(String batchId, String platformStoreId) {
        progressCache.markStoreCompleted(batchId, platformStoreId);
    }
}
