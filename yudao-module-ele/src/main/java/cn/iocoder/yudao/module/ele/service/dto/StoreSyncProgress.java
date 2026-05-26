package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StoreSyncProgress {
    private String batchId;
    private String platformStoreId;
    private String storeName;
    private String status; 
    private Map<Integer, Long> apiStatusCounts;
    private Long totalApiCount;

    private Map<Integer, Long> savedStatusCounts;
    private Long totalSavedCount;

    private Map<Integer, Integer> pageCounts;
    private Integer totalPages;

    private Double discrepancyRate;
    private String reconciliationStatus;

    private Integer retryCount;
    private Long lastRetryTime;

    private List<SyncErrorDTO> pullErrors;
    private List<SyncErrorDTO> saveErrors;
    private List<SyncErrorDTO> reconciliationErrors;

    private Long startTime;
    private Long endTime;
    private Long elapsedSeconds;
}
