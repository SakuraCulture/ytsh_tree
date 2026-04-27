package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EleStoreGoodsShadowUpsertReqBO {

    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String spuCode;
    private String skuCode;
    private String subSkuCode;
    private String title;
    private String mainPic;
    private String subPics;
    private String frontCategory;
    private String brandName;
    private String specification;
    private BigDecimal salePrice;
    private String posStatus;
    private Integer isActive;
    private String rawPayload;
}
