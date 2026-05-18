package cn.iocoder.yudao.module.ele.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EleStoreInventoryImportRespVO {

    private Integer formalSuccessCount = 0;
    private Integer shadowSuccessCount = 0;
    private Integer governanceCount = 0;
    private Integer failureCount = 0;
    private List<FailureItem> failureList = new ArrayList<>();

    @Data
    public static class FailureItem {
        private Integer rowNo;
        private String skuCode;
        private String message;
    }
}
