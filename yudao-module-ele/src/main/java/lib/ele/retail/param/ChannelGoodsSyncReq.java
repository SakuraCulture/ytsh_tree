package lib.ele.retail.param;

import lombok.Data;

@Data
public class ChannelGoodsSyncReq {

    private String merchant_code;
    private String store_code;
    private String channel_type;
    private String out_shop_id;
    private Boolean multi_spec;
    private Integer status;
    private CategoryDTO[] front_category_list;
    private String title;
    private String main_pic;
    private String[] sub_pics;
    private String brand_name;
    private String brand_id;
    private String description;
    private String alias_name;
    private String picture_content;
    private String spu_code;
    private ChannelSkuSyncReq[] sku_list;
}
