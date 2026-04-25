package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EleCompensateProgressDTO implements Serializable {

    private String status;
    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private Long startTime;
    private Long endTime;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
}
