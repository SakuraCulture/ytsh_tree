package cn.iocoder.yudao.module.ele.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EleSkuInventoryBatchQueryReqBO {

    private String platformStoreId;
    private Long platformId;
    private String storeId;
    private String merchantCode;
    private String erpStoreCode;
    private List<String> skuCodes;
    private List<String> subSkuCodes;
}
