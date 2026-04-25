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
 * 仓库 DO
 *
 * @author 彼岸花
 */
@TableName("warehouse_table")
@KeySequence("warehouse_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDO extends TenantBaseDO {

    /**
     * 仓库ID
     */
    @TableId(type = IdType.INPUT)
    private String warehouseId;
    /**
     * 仓库编码
     */
    private String warehouseCode;
    /**
     * 仓库名称
     */
    private String warehouseName;
    /**
     * 仓库类型
     */
    private String warehouseType;
    /**
     * 行政区划代码
     */
    private String regionCode;
    /**
     * 详细地址
     */
    private String address;
    /**
     * 仓库状态(0停用1正常)
     */
    private Integer warehouseStatus;
    /**
     * 是否默认仓(0否1是)
     */
    private Integer isDefault;

}
