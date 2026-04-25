package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 仓库商品 DO
 *
 * @author 彼岸花
 */
@TableName("warehouse_product_table")
@KeySequence("warehouse_product_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseProductDO extends TenantBaseDO {

    /**
     * 仓库商品ID
     */
    @TableId
    private Long warehouseProductId;
    /**
     * 仓库ID
     */
    private String warehouseId;
    /**
     * 商品SKU ID
     */
    private Long productSkuId;
    /**
     * 该仓库采购价
     */
    private BigDecimal warehouseProductCostPrice;
    /**
     * 库位编码
     */
    private String warehouseProductLocation;
    /**
     * 首次有库存日期
     */
    private LocalDate warehouseProductFirstDate;
    /**
     * 最近入库日期
     */
    private LocalDate warehouseProductLastDate;

}
