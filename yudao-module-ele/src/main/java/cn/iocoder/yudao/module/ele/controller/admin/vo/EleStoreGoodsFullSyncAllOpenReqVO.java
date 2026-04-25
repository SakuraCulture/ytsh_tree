package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 饿了么所有开业门店商品全量同步 Request VO")
@Data
public class EleStoreGoodsFullSyncAllOpenReqVO {

    @Schema(description = "是否测试模式", example = "false")
    private Boolean testMode;
}
