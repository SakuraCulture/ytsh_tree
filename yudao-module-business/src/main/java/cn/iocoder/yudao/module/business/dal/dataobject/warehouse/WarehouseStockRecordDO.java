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

/**
 * 仓库库存流水 DO
 */
@TableName("warehouse_stock_record_table")
@KeySequence("warehouse_stock_record_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockRecordDO extends TenantBaseDO {

    @TableId
    private Long stockRecordId;
    private String warehouseId;
    private Long warehouseProductId;
    private Long productSkuId;
    private String bizType;
    private Long bizId;
    private Long bizItemId;
    private String bizNo;
    private Integer changeQty;
    private Integer afterQty;

}
