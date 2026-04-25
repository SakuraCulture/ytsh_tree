package cn.iocoder.yudao.module.business.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 商品类目表（三级树形结构） Response VO")
@Data
@ExcelIgnoreUnannotated
public class CategoryTableRespVO {

    @Schema(description = "类目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "25050")
    @ExcelProperty("类目ID")
    private Long categoryId;

    @Schema(description = "类目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @ExcelProperty("类目名称")
    private String categoryName;

    @Schema(description = "父类目ID（0表示一级类目）", example = "14807")
    @ExcelProperty("父类目ID（0表示一级类目）")
    private Long parentId;

    @Schema(description = "层级（1一级/2二级/3三级）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("层级（1一级/2二级/3三级）")
    private Integer categoryLevel;

    @Schema(description = "类目路径（如：1/2/3）")
    @ExcelProperty("类目路径（如：1/2/3）")
    private String categoryPath;

    @Schema(description = "类目图标URL")
    @ExcelProperty("类目图标URL")
    private String categoryIcon;

    @Schema(description = "类目配图URL")
    @ExcelProperty("类目配图URL")
    private String categoryImage;

    @Schema(description = "同级排序")
    @ExcelProperty("同级排序")
    private Integer sortOrder;

    @Schema(description = "是否叶子类目（0否 1是）")
    @ExcelProperty("是否叶子类目（0否 1是）")
    private Integer isLeaf;

    @Schema(description = "状态（0禁用 1启用）", example = "2")
    @ExcelProperty("状态（0禁用 1启用）")
    private Integer status;

    @Schema(description = "父类目名称", example = "水果")
    @ExcelProperty("父类目名称")
    private String parentCategoryName;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}