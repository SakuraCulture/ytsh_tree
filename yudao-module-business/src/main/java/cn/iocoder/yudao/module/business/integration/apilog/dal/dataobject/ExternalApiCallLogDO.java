package cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("biz_external_api_call_log")
@KeySequence("biz_external_api_call_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiCallLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformCode;
    private String apiCode;
    private String apiName;
    private String bizType;
    private String bizId;
    private String bizNo;
    private String traceId;
    private String externalTraceId;
    private String requestId;
    private String requestUrl;
    private String requestMethod;
    private String requestBody;
    private String responseBody;
    private Boolean success;
    private String resultCode;
    private String resultMsg;
    private Integer durationMs;
    private String merchantCode;
    private String platformStoreId;
    private String erpStoreCode;
    private String orderId;
    private String channelOrderId;
}
