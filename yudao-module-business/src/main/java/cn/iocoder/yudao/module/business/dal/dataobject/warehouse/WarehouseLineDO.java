package cn.iocoder.yudao.module.business.dal.dataobject.warehouse;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("warehouse_line")
@KeySequence("warehouse_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseLineDO extends BaseDO {

    @TableId
    private Long lineId;
    private String warehouseId;
    private String lineCode;
    private String lineName;
    private String orderWeekdays;
    private Integer lineStatus;
    private String remark;
}
