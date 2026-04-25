package cn.iocoder.yudao.module.business.service.warehouse.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仓库库存流水创建 Request BO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockRecordCreateReqBO {

    @NotNull(message = "仓库ID不能为空")
    private String warehouseId;

    @NotNull(message = "仓库商品ID不能为空")
    private Long warehouseProductId;

    @NotNull(message = "商品SKU ID不能为空")
    private Long productSkuId;

    @NotNull(message = "变动数量不能为空")
    private Integer changeQty;

    @NotNull(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    @NotNull(message = "业务项ID不能为空")
    private Long bizItemId;

    @NotNull(message = "业务单号不能为空")
    private String bizNo;

}
