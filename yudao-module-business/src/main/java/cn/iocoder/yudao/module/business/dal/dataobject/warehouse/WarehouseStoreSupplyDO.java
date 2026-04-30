package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("warehouse_store_supply")
@KeySequence("warehouse_store_supply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseStoreSupplyDO extends BaseDO {

    @TableId
    private Long id;
    private String warehouseId;
    private String storeId;
    private Integer isPrimary;
    private Integer supplyStatus;
    private String remark;
}
