package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 饿了么门店商品待治理池状态更新 Request VO")
@Data
public class EleStoreGoodsGovernancePoolStatusReqVO {

    @Schema(description = "备注")
    private String remark;
}
