package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 标签维度新增/修改 Request VO")
@Data
public class TagDimensionSaveReqVO {

    @Schema(description = "标签维度编号", example = "1024")
    private Long id;

    @Schema(description = "对象域", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    @NotEmpty(message = "对象域不能为空")
    private String domainType;

    @Schema(description = "父级标签维度编号，0 表示一级", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "父级标签维度编号不能为空")
    private Long parentId;

    @Schema(description = "层级，1/2/3", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "层级不能为空")
    private Integer level;

    @Schema(description = "标签维度名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "基础属性")
    @NotEmpty(message = "标签维度名称不能为空")
    private String name;

    @Schema(description = "标签维度编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "base")
    @NotEmpty(message = "标签维度编码不能为空")
    private String code;

    @Schema(description = "排序", example = "10")
    private Integer sort;

    @Schema(description = "状态，0 禁用，1 启用", example = "1")
    private Integer status;

    @Schema(description = "描述", example = "商品基础属性")
    private String description;

}
