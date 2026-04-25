package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - 虚拟标签新增/修改 Request VO")
@Data
public class TagVirtualSaveReqVO {

    @Schema(description = "虚拟标签编号", example = "1024")
    private Long id;

    @Schema(description = "对象域", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    @NotEmpty(message = "对象域不能为空")
    private String domainType;

    @Schema(description = "虚拟标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值商品")
    @NotEmpty(message = "虚拟标签名称不能为空")
    private String name;

    @Schema(description = "虚拟标签编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_value_product")
    @NotEmpty(message = "虚拟标签编码不能为空")
    private String code;

    @Schema(description = "表达式 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "表达式 JSON 不能为空")
    private String expressionJson;

    @Schema(description = "表达式摘要", example = "近30天销量大于100")
    private String expressionSummary;

    @Schema(description = "使用场景", example = "首页推荐")
    private String usageScenario;

    @Schema(description = "状态，0 禁用，1 启用", example = "1")
    private Integer status;

}
