package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 仓库库存 DO
 *
 * @author 彼岸花
 */
@TableName("warehouse_stock_table")
@KeySequence("warehouse_stock_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockDO extends TenantBaseDO {

    /**
     * 仓库库存ID
     */
    @TableId
    private Long warehouseStockId;
    /**
     * 关联 warehouse_product
     */
    private Long warehouseProductId;
    /**
     * 库存数量
     */
    @TableField("warehouse_stock_qty")
    private Integer warehouseStockQty;
    /**
     * 可用量
     */
    @TableField("warehouse_stock_available_qty")
    private Integer warehouseStockAvailableQty;
    /**
     * 在途数量
     */
    @TableField("warehouse_stock_transit_qty")
    private Integer warehouseStockTransitQty;
    /**
     * 冻结库存
     */
    @TableField("warehouse_stock_frozen_qty")
    private Integer warehouseStockFrozenQty;
    /**
     * 缺货时长(小时)
     */
    @TableField("warehouse_stock_outstock_hours")
    private Integer warehouseStockOutstockHours;

}
