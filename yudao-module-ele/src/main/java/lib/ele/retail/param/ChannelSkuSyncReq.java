package lib.ele.retail.param;

import lombok.Data;

@Data
public class ChannelSkuSyncReq {

    private String sub_sku_code;
    private String[] barcode_list;
    private Long sale_price;
    private String weight;
    private String specification;
    private String inventory_unit;
    private String sku_code;
    private String channel_backend_category_id;
    private GoodsLifeCycleDTO goods_life_cycle;
    private Integer period;
}
