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
 * 仓库采购订单 DO
 */
@TableName("warehouse_purchase_order_table")
@KeySequence("warehouse_purchase_order_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehousePurchaseDO extends TenantBaseDO {

    @TableId
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private String supplierId;
    private String supplierName;
    private String warehouseId;
    private LocalDate purchaseDate;
    private String orderStatus;
    private String receiveStatus;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private Integer totalInboundQty;
    private Integer diffQty;
    private Integer returnQty;
    private String purchaser;
    private String receiveAddress;
    private LocalDate auditDate;
    private String remark;

}
