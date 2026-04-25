package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 标签维度 Response VO")
@Data
public class TagDimensionRespVO {

    @Schema(description = "标签维度编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "对象域", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    private String domainType;

    @Schema(description = "父级标签维度编号，0 表示一级", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Long parentId;

    @Schema(description = "层级，1/2/3", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer level;

    @Schema(description = "标签维度名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "基础属性")
    private String name;

    @Schema(description = "标签维度编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "base")
    private String code;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer sort;

    @Schema(description = "状态，0 禁用，1 启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "描述", example = "商品基础属性")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
