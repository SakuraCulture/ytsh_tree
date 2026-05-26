package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


@TableName("ele_order_tracking")
@KeySequence("ele_order_tracking_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EleOrderTrackingDO extends BaseDO {

    
    @TableId
    private Long id;

    
    private String orderId;

    
    private String platformStoreId;

    
    private String merchantCode;

    
    private String erpStoreCode;

    
    private String channelOrderId;

    
    private Integer orderStatus;

    
    private Long orderCreateTime;

    
    private Long lastPushTime;

    
    private Integer lastPushStatus;

    
    private String trackingStatus;

    
    private String alertLevel;

    
    private Integer alertShown;

    
    private String remark;
}
