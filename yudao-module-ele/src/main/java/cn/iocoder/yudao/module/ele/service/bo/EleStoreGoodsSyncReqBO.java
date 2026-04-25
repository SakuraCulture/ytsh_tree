package cn.iocoder.yudao.module.ele.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EleStoreGoodsSyncReqBO {

    private String traceId;

    private String ticket;

    private String apiCode;

    private String apiName;

    private String merchantCode;

    private String erpStoreCode;

    private String platformStoreId;

    private String skuCode;

    private String subSkuCode;

    private String spuCode;

    private String goodsLevel;

    private String operationType;

    private String storeProductPosStatus;

    private BigDecimal storeProductPrice;

    private Integer storeProductIsActive;

    private Integer pageNo;

    private Integer pageSize;

    private Integer dataCount;

    private String requestBody;

    private String responseBody;

    private String rawPayload;

    private Boolean testMode;
}
