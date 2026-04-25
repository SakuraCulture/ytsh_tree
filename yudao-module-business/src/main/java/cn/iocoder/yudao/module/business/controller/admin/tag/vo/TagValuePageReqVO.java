package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 标签值分页 Request VO")
@Data
public class TagValuePageReqVO extends PageParam {

    @Schema(description = "对象域", example = "PRODUCT")
    private String domainType;

    @Schema(description = "所属标签维度编号", example = "1")
    private Long dimensionId;

    @Schema(description = "标签值名称", example = "高价值用户")
    private String name;

    @Schema(description = "标签值编码", example = "high_value")
    private String code;

    @Schema(description = "打标方式", example = "MANUAL")
    private String tagMethod;

    @Schema(description = "状态，0 禁用，1 启用", example = "1")
    private Integer status;

}
