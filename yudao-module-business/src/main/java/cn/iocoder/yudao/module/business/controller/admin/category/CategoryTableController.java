package cn.iocoder.yudao.module.business.controller.admin.category;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.business.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.category.CategoryTableDO;
import cn.iocoder.yudao.module.business.service.category.CategoryTableService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台 - 商品类目表（三级树形结构）
 *
 * @author 彼岸花
 */
@Tag(name = "管理后台 - 商品类目表（三级树形结构）")
@RestController
@RequestMapping("/business/category-table")
@Validated
public class CategoryTableController {

    @Resource
    private CategoryTableService categoryTableService;

    @PostMapping("/create")
    @Operation(summary = "创建商品类目表（三级树形结构）")
    @PreAuthorize("@ss.hasPermission('business:category-table:create')")
    public CommonResult<Long> createCategoryTable(@Valid @RequestBody CategoryTableSaveReqVO createReqVO) {
        return success(categoryTableService.createCategoryTable(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新商品类目表（三级树形结构）")
    @PreAuthorize("@ss.hasPermission('business:category-table:update')")
    public CommonResult<Boolean> updateCategoryTable(@Valid @RequestBody CategoryTableSaveReqVO updateReqVO) {
        categoryTableService.updateCategoryTable(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商品类目表（三级树形结构）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:category-table:delete')")
    public CommonResult<Boolean> deleteCategoryTable(@RequestParam("id") Long id) {
        categoryTableService.deleteCategoryTable(id);
        return success(true);
    }

    @DeleteMapping("/delete-by-ids")
    @Operation(summary = "批量删除商品类目表（三级树形结构）")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('business:category-table:delete')")
    public CommonResult<Integer> deleteCategoryTableByIds(@RequestParam("ids") List<Long> ids) {
        int count = categoryTableService.deleteCategoryTableByIds(ids);
        return success(count);
    }


    @GetMapping("/get")
    @Operation(summary = "获得商品类目表（三级树形结构）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:category-table:query')")
    public CommonResult<CategoryTableRespVO> getCategoryTable(@RequestParam("id") Long id) {
        CategoryTableDO categoryTable = categoryTableService.getCategoryTable(id);
        return success(BeanUtils.toBean(categoryTable, CategoryTableRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得商品类目表（三级树形结构）列表")
    @PreAuthorize("@ss.hasPermission('business:category-table:query')")
    public CommonResult<List<CategoryTableRespVO>> getCategoryTableList(@Valid CategoryTableListReqVO listReqVO) {
        List<CategoryTableDO> list = categoryTableService.getCategoryTableList(listReqVO);
        return success(BeanUtils.toBean(list, CategoryTableRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出商品类目表（三级树形结构） Excel")
    @PreAuthorize("@ss.hasPermission('business:category-table:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCategoryTableExcel(@Valid CategoryTableListReqVO listReqVO,
              HttpServletResponse response) throws IOException {
        List<CategoryTableDO> list = categoryTableService.getCategoryTableList(listReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "商品类目表（三级树形结构）.xls", "数据", CategoryTableRespVO.class,
                        BeanUtils.toBean(list, CategoryTableRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入类目模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<CategoryTableImportReqVO> list = Arrays.asList(
                CategoryTableImportReqVO.builder().categoryName("水果").parentCategoryName("").categoryLevel(1).sortOrder(1).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("蔬菜").parentCategoryName("").categoryLevel(1).sortOrder(2).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("苹果").parentCategoryName("水果").categoryLevel(2).sortOrder(1).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("香蕉").parentCategoryName("水果").categoryLevel(2).sortOrder(2).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("白菜").parentCategoryName("蔬菜").categoryLevel(2).sortOrder(1).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("红富士苹果").parentCategoryName("苹果").categoryLevel(3).sortOrder(1).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("黄香蕉").parentCategoryName("香蕉").categoryLevel(3).sortOrder(1).status(1).build(),
                CategoryTableImportReqVO.builder().categoryName("大白菜").parentCategoryName("白菜").categoryLevel(3).sortOrder(1).status(1).build()
        );
        ExcelUtils.write(response, "商品类目导入模板.xls", "类目列表", CategoryTableImportReqVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入类目")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "file", description = "Excel 文件", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    public CommonResult<CategoryTableImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<CategoryTableImportReqVO> list = ExcelUtils.read(file, CategoryTableImportReqVO.class);
        return success(categoryTableService.importCategoryTableList(list, updateSupport));
    }

}