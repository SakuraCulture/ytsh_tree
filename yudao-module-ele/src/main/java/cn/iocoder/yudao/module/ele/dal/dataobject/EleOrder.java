package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("ele_order")
@Data
public class EleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderId;

    private String platformStoreId;

    private String merchantCode;

    private String erpStoreCode;

    private Integer status;

    private Long createTime;

    private Long payTime;

    private String channelSourceName;

    private String buyerName;

    private String buyerPhone;

    private String buyerAddress;

    private String deliveryName;

    private String deliveryPhone;

    private String deliveryPlatform;

    private Integer deliveryType;

    private Integer deliveryStatus;

    private Integer totalFee;

    private Integer payFee;

    private Integer discountFee;

    private Integer deliveryFee;

    private Integer postFee;

    private Integer packageFee;

    private Integer platformCommissionFee;

    private String remark;

    private String channelSourceId;

    private String channelOrderId;

    private String channelType;

    private String storeCode;

    private String longitude;

    private String latitude;

    private String subOrdersJson;

    private String discountsJson;

    private LocalDateTime syncTime;

    private Long updateTime;
}