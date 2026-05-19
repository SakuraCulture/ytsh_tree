package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class SyncErrorDTO {
    private String errorCode;
    private String errorDesc;
    private Integer status;
    private List<String> orderIds;
    private Long timestamp;
    private Boolean retryable;
    private String detail;

    public static SyncErrorDTO create(String errorCode, String errorDesc, Integer status, String detail, boolean retryable) {
        SyncErrorDTO error = new SyncErrorDTO();
        error.setErrorCode(errorCode);
        error.setErrorDesc(errorDesc);
        error.setStatus(status);
        error.setDetail(detail);
        error.setRetryable(retryable);
        error.setTimestamp(System.currentTimeMillis());
        return error;
    }

    public static SyncErrorDTO create(String errorCode, String errorDesc, Integer status, List<String> orderIds, String detail, boolean retryable) {
        SyncErrorDTO error = new SyncErrorDTO();
        error.setErrorCode(errorCode);
        error.setErrorDesc(errorDesc);
        error.setStatus(status);
        error.setOrderIds(orderIds);
        error.setDetail(detail);
        error.setRetryable(retryable);
        error.setTimestamp(System.currentTimeMillis());
        return error;
    }
}
