package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 饿了么门店库存批量任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EleStoreInventoryBatchTaskPageReqVO extends PageParam {

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "任务范围")
    private String scope;

    @Schema(description = "任务状态")
    private String status;
}
