package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 标签值 Response VO")
@Data
public class TagValueRespVO {

    @Schema(description = "标签值编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属标签维度编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long dimensionId;

    @Schema(description = "标签值名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值用户")
    private String name;

    @Schema(description = "标签值编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_value")
    private String code;

    @Schema(description = "打标方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUAL")
    private String tagMethod;

    @Schema(description = "数据来源", example = "运营后台")
    private String dataSource;

    @Schema(description = "更新频率", example = "每日")
    private String updateFrequency;

    @Schema(description = "逻辑说明", example = "按近30天消费金额判断")
    private String logicDescription;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer sort;

    @Schema(description = "状态，0 禁用，1 启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
