package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 饿了么门店商品全量同步任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EleStoreGoodsFullSyncTaskPageReqVO extends PageParam {

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "同步范围")
    private String scope;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;
}
