package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("warehouse_line_store")
@KeySequence("warehouse_line_store_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseLineStoreDO extends BaseDO {

    @TableId
    private Long id;
    private Long lineId;
    private String storeId;
    private Integer sortNo;
}
