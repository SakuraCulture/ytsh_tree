package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 虚拟标签 Response VO")
@Data
public class TagVirtualRespVO {

    @Schema(description = "虚拟标签编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "对象域", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    private String domainType;

    @Schema(description = "虚拟标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值商品")
    private String name;

    @Schema(description = "虚拟标签编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_value_product")
    private String code;

    @Schema(description = "表达式 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expressionJson;

    @Schema(description = "表达式摘要", example = "近30天销量大于100")
    private String expressionSummary;

    @Schema(description = "使用场景", example = "首页推荐")
    private String usageScenario;

    @Schema(description = "状态，0 禁用，1 启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
