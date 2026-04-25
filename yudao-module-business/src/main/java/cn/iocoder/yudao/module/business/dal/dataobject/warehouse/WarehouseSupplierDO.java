package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
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
 * 仓库供应商 DO
 *
 * @author 彼岸花
 */
@TableName("warehouse_supplier_table")
@KeySequence("warehouse_supplier_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseSupplierDO extends TenantBaseDO {

    /**
     * 供应商ID
     */
    @TableId(type = IdType.INPUT)
    private String supplierId;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 供应商分类
     */
    private String categoryName;
    /**
     * 负责人
     */
    private String managerName;
    /**
     * 电话
     */
    private String phone;
    /**
     * 联系地址
     */
    private String address;
    /**
     * 付款方式
     */
    private String paymentMethod;
    /**
     * 账期天数
     */
    private Integer paymentDays;
    /**
     * 供应商状态(0停用1正常)
     */
    private Integer supplierStatus;

}
