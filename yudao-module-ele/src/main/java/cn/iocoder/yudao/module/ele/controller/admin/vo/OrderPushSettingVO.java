package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单推送设置")
public class OrderPushSettingVO {
    
    @Schema(description = "是否启用推送")
    private Boolean orderPushEnabled;
    
    @Schema(description = "推送订单状态(逗号分隔)")
    private String orderPushTypes;
    
    @Schema(description = "是否开启声音")
    private Boolean orderPushSound;
    
    @Schema(description = "是否开启桌面通知")
    private Boolean orderPushDesktop;
}
