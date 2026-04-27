package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 可选标签值 Response VO")
@Data
public class TagSelectableValueRespVO {

    @Schema(description = "标签值编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tagValueId;

    @Schema(description = "标签值编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_value_product")
    private String tagValueCode;

    @Schema(description = "标签值名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值商品")
    private String tagValueName;

    @Schema(description = "所属标签维度编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long dimensionId;

    @Schema(description = "所属标签维度名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "功能角色")
    private String dimensionName;

    @Schema(description = "三级维度路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "商品属性 / 商品角色 / 功能角色")
    private String dimensionPath;

    @Schema(description = "状态，0 禁用，1 启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

}
