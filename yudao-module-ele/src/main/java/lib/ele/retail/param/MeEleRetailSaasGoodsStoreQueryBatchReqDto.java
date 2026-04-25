package lib.ele.retail.param;

import lombok.Data;

@Data
public class MeEleRetailSaasGoodsStoreQueryBatchReqDto {

    private String merchant_code;
    private String erp_store_code;
    private String[] sku_code_list;
    private Integer page_size;
    private Integer page_no;
}
