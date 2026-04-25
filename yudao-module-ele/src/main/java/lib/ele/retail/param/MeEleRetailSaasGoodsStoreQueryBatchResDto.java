package lib.ele.retail.param;

import lombok.Data;

@Data
public class MeEleRetailSaasGoodsStoreQueryBatchResDto {

    private String merchant_code;
    private String store_code;
    private ChannelGoodsSyncReq[] channelGoodsSyncReqList;
    private Integer page;
    private Integer total;
    private Integer page_size;
}
