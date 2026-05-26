package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店库存 DO
 *
 * @author 彼岸花
 */
@TableName("store_stock_table")
@KeySequence("store_stock_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStockDO extends BaseDO {

    /**
     * 库存ID
     */
    @TableId(type = IdType.AUTO)
    private Long storeStockId;

    /**
     * 门店商品ID
     */
    private String storeProductId;

    /**
     * 库存数量
     */
    @TableField("store_stock_qty")
    private Integer storeStockQuantity;

    /**
     * 可用量
     */
    @TableField("store_stock_available_qty")
    private Integer storeStockAvailableQuantity;

    /**
     * 在途数量
     */
    @TableField("store_stock_transit_qty")
    private Integer storeStockTransitQuantity;

    /**
     * 冻结库存
     */
    @TableField("store_stock_frozen_qty")
    private Integer storeStockFrozenQuantity;

    /**
     * 缺货时长（小时）
     */
    @TableField("store_stock_outstock_hours")
    private Integer storeStockOutstockHours;

}
