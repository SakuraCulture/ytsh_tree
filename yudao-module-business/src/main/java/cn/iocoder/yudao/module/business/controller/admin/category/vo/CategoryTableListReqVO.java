package cn.iocoder.yudao.module.business.controller.admin.category.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 商品类目表（三级树形结构）列表 Request VO")
@Data
public class CategoryTableListReqVO {

    @Schema(description = "类目编码")
    private String categoryCode;

    @Schema(description = "类目名称", example = "赵六")
    private String categoryName;

    @Schema(description = "父类目ID（0表示一级类目）", example = "14807")
    private Long parentId;

    @Schema(description = "层级（1一级/2二级/3三级）")
    private Integer categoryLevel;

    @Schema(description = "类目路径（如：1/2/3）")
    private String categoryPath;

    @Schema(description = "类目图标URL")
    private String categoryIcon;

    @Schema(description = "类目配图URL")
    private String categoryImage;

    @Schema(description = "同级排序")
    private Integer sortOrder;

    @Schema(description = "是否叶子类目（0否 1是）")
    private Integer isLeaf;

    @Schema(description = "状态（0禁用 1启用）", example = "2")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}