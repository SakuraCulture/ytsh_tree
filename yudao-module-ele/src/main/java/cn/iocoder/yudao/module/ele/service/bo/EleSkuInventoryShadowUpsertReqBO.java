package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

@Data
public class EleSkuInventoryShadowUpsertReqBO {

    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String skuCode;
    private String subSkuCode;
    private String matchedProductSkuId;
    private String matchedStoreProductId;
    private Integer availableForSale;
    private Integer reservedAmount;
    private Integer physicalStockTotalAmount;
    private Integer physicalStockAvailableAmount;
    private Integer physicalStockOccupiedAmount;
    private Integer physicalStockIntransitAmount;
    private String ownerCode;
    private String ownerName;
    private String rawPayload;
}
