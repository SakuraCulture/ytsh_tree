package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 标签维度列表 Request VO")
@Data
public class TagDimensionListReqVO {

    @Schema(description = "对象域", example = "PRODUCT")
    private String domainType;

    @Schema(description = "父级标签维度编号", example = "0")
    private Long parentId;

    @Schema(description = "层级，1/2/3", example = "1")
    private Integer level;

}
