package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EleSkuInventoryBatchQueryRespDTO {

    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private Integer requestSkuCount = 0;
    private Integer responseRowCount = 0;
    private Integer formalSuccessCount = 0;
    private Integer shadowSuccessCount = 0;
    private Integer governanceCount = 0;
    private Integer missingRowCount = 0;
    private Integer failureCount = 0;
    private String status;
    private List<String> errorDetails = new ArrayList<>();
    private List<InventoryRowDTO> inventoryRows = new ArrayList<>();

    @Data
    public static class InventoryRowDTO {

        private String skuCode;
        private String subSkuCode;
        private Integer availableForSale;
        private Integer reservedAmount;
        private Integer physicalStockTotalAmount;
        private Integer physicalStockAvailableAmount;
        private Integer physicalStockOccupiedAmount;
        private Integer physicalStockIntransitAmount;
        private String ownerCode;
        private String ownerName;
        private String persistStatus;
        private String reasonCode;
    }
}
