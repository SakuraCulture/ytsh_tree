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

/**
 * 仓库采购订单明细 DO
 */
@TableName("warehouse_purchase_order_detail_table")
@KeySequence("warehouse_purchase_order_detail_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehousePurchaseDetailDO extends TenantBaseDO {

    @TableId
    private Long detailId;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long productSkuId;
    private String productSkuCode;
    private String productSkuName;
    private Integer purchaseQty;
    private Integer boxQty;
    private Integer standardBoxQty;
    private BigDecimal purchasePrice;
    private BigDecimal purchaseAmount;
    private Integer inboundQty;
    private Integer returnQty;
    private Integer diffQty;

}
