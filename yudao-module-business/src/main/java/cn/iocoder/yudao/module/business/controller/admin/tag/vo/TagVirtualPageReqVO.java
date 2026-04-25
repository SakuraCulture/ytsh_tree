package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 虚拟标签分页 Request VO")
@Data
public class TagVirtualPageReqVO extends PageParam {

    @Schema(description = "对象域", example = "PRODUCT")
    private String domainType;

    @Schema(description = "虚拟标签名称", example = "高价值商品")
    private String name;

    @Schema(description = "虚拟标签编码", example = "high_value_product")
    private String code;

    @Schema(description = "状态，0 禁用，1 启用", example = "1")
    private Integer status;

}
