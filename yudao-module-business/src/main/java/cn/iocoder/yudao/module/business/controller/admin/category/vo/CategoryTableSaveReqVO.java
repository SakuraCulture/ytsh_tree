package cn.iocoder.yudao.module.business.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 商品类目表（三级树形结构）新增/修改 Request VO")
@Data
public class CategoryTableSaveReqVO {

    @Schema(description = "类目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "25050")
    private Long categoryId;

    @Schema(description = "类目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @NotEmpty(message = "类目名称不能为空")
    private String categoryName;

    @Schema(description = "父类目ID（0表示一级类目）", example = "14807")
    private Long parentId;

    @Schema(description = "层级（1一级/2二级/3三级）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "层级（1一级/2二级/3三级）不能为空")
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

}