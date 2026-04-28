package cn.iocoder.yudao.module.business.service.store.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StoreProductShadowRowBO {

    private Long shadowId;
    private String storeId;
    private String erpStoreCode;
    private String platformStoreId;
    private String skuCode;
    private String spuCode;
    private String productName;
    private String specification;
    private BigDecimal price;
    private Integer posStatus;
    private Integer isActive;
    private String matchStatus;
    private LocalDateTime createTime;
}
