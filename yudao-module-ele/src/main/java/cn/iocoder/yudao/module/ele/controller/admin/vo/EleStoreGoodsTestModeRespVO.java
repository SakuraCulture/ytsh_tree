package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理后台 - 饿了么商品同步测试模式 Response VO")
public class EleStoreGoodsTestModeRespVO {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "是否开启")
    private Boolean enabled;
}
