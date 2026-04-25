package cn.iocoder.yudao.module.business.controller.admin.category.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTableImportReqVO {

    @ExcelProperty("类目名称")
    private String categoryName;

    @ExcelProperty("父类目名称")
    private String parentCategoryName;

    @ExcelProperty("层级(1一级/2二级/3三级)")
    private Integer categoryLevel;

    @ExcelProperty("同级排序")
    private Integer sortOrder;

    @ExcelProperty("状态(0禁用 1启用)")
    private Integer status;

}
