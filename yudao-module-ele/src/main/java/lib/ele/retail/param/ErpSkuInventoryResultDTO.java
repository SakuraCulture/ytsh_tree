package lib.ele.retail.param;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class ErpSkuInventoryResultDTO {

    @JSONField(name = "skuCode")
    private String sku_code;
    @JSONField(name = "subSkuCode")
    private String sub_sku_code;
    @JSONField(name = "availableForSale")
    private Integer available_for_sale;
    @JSONField(name = "reservedAmount")
    private Integer reserved_amount;
    @JSONField(name = "physicalStockTotalAmount")
    private Integer physical_stock_total_amount;
    @JSONField(name = "physicalStockAvailableAmount")
    private Integer physical_stock_available_amount;
    @JSONField(name = "physicalStockOccupiedAmount")
    private Integer physical_stock_occupied_amount;
    @JSONField(name = "physicalStockIntransitAmount")
    private Integer physical_stock_intransit_amount;
    @JSONField(name = "ownerInfo")
    private OwnerInfo owner_info;
}
