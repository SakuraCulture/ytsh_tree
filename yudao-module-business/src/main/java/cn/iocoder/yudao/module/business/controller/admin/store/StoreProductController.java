package cn.iocoder.yudao.module.business.controller.admin.store;

/**
 * 门店商品 Controller
 *
 * ==============================================================
 * 【Why - 为什么要做门店商品管理】
 * ==============================================================
 *
 * 设计决策：为什么要单独管理门店商品？
 *
 * 方案A（通用商品表）：所有商品使用同一张表，通过 store_id 字段区分
 * - 优点：统一管理，查询简单
 * - 缺点：
 *   1. 门店商品有特殊的归属、价格、上架状态等属性
 *   2. 不同门店可能有不同的商品配置
 *   3. 门店商品与库存、订单有强关联
 *
 * 方案B（独立表）：拆出 store_product 表
 * - 优点：
 *   1. 支持门店维度的商品管理
 *   2. 便于门店独立定价、上架
 *   3. 支持门店商品与通用商品解耦
 * - 缺点：需要关联查询
 *
 * 最终选择：方案B
 * - 业务上门店需要独立管理商品配置
 * - 支持多门店、多商品、多价格场景
 *
 * ==============================================================
 * 【What - 这个模块做什么】
 * ==============================================================
 * - 门店商品 CRUD 操作
 * - 门店商品导入导出
 * - 门店库存查询
 *
 * ==============================================================
 * 【Constraints - 约束条件】
 * ==============================================================
 * - 门店商品唯一性：同一门店的同一 SKU 只能有一条入店记录
 * - 删除前检查库存：库存不为零时不能删除
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【陷阱】门店商品与通用商品的关联问题
 * - 【陷阱】库存同步时机问题
 *
 * @author 彼岸花
 * @see StoreProductService
 */
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.hutool.core.util.StrUtil;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreStockRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreProductService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 门店商品管理接口
 *
 * 【What】提供门店商品的 CRUD、导入导出、库存查询等功能
 *
 * 【Constraints】
 * - 门店商品唯一性约束：(storeId + productSkuId + ownership) 组合唯一
 * - 删除前检查库存不为零
 *
 * 【Pitfalls】
 * - 导入时不校验重复可能导致数据覆盖
 */
@Tag(name = "管理后台 - 门店商品")
@RestController
@RequestMapping("/store-product")
@Validated
public class StoreProductController {

    @Resource
    private StoreProductService storeProductService;

    /**
     * 创建门店商品
     *
     * 【What】
     * 1. 校验同一门店的同一 SKU + 归属组合是否已存在
     * 2. 插入门店商品记录
     *
     * 【Constraints】
     * - 入店商品必须唯一：(storeId + productSkuId + 入店) 不能重复
     *
     * 【Pitfalls】
     * - 【陷阱】只校验了入店归属，未校验其他归属类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    @PostMapping("/create")
    @Operation(summary = "创建门店商品")
    @PreAuthorize("@ss.hasPermission('business:store-product:create')")
    public CommonResult<String> createStoreProduct(@Valid @RequestBody StoreProductSaveReqVO createReqVO) {
        return success(storeProductService.createStoreProduct(createReqVO));
    }

    /**
     * 更新门店商品
     *
     * 【What】更新门店商品信息
     *
     * 【Constraints】
     * - 更新前校验商品存在性
     *
     * @param updateReqVO 更新信息
     * @return 成功标志
     */
    @PutMapping("/update")
    @Operation(summary = "更新门店商品")
    @PreAuthorize("@ss.hasPermission('business:store-product:update')")
    public CommonResult<Boolean> updateStoreProduct(@Valid @RequestBody StoreProductSaveReqVO updateReqVO) {
        storeProductService.updateStoreProduct(updateReqVO);
        return success(true);
    }

    /**
     * 删除门店商品
     *
     * 【What】
     * 1. 校验商品存在性
     * 2. 检查库存不为零
     * 3. 删除门店商品
     *
     * 【Constraints】
     * - 库存不为零时不能删除
     *
     * 【Pitfalls】
     * - 【陷阱】库存检查可能导致删除失败，需先处理库存
     *
     * @param id 编号
     * @return 成功标志
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除门店商品")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:store-product:delete')")
    public CommonResult<Boolean> deleteStoreProduct(@PathVariable("id") String id) {
        storeProductService.deleteStoreProduct(id);
        return success(true);
    }

    /**
     * 批量删除门店商品
     *
     * 【What】批量删除门店商品，逐条校验库存
     *
     * @param ids 编号列表
     * @return 成功标志
     */
    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除门店商品")
    @Parameter(name = "ids", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:store-product:delete')")
    public CommonResult<Boolean> deleteStoreProductList(@RequestParam("ids") List<String> ids) {
        storeProductService.deleteStoreProductList(ids);
        return success(true);
    }

    /**
     * 获得门店商品
     *
     * 【What】根据 ID 查询门店商品详情
     *
     * @param id 编号
     * @return 门店商品信息
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "获得门店商品")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<StoreProductRespVO> getStoreProduct(@PathVariable("id") String id) {
        return success(storeProductService.getStoreProduct(id));
    }

    /**
     * 获得门店商品分页
     *
     * 【What】支持分页、关键词搜索的门店商品列表查询
     *
     * @param pageReqVO 分页查询
     * @return 门店商品分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得门店商品分页")
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<PageResult<StoreProductRespVO>> getStoreProductPage(@Valid StoreProductPageReqVO pageReqVO) {
        return success(storeProductService.getStoreProductPage(pageReqVO));
    }

    /**
     * 获得门店商品简单列表
     *
     * 【What】返回门店商品的简化信息
     *
     * 【Why - 为什么要区分 storeId 和空？】
     * - 传入 storeId：返回该门店的商品列表
     * - 不传 storeId：返回所有门店商品
     *
     * @param storeId 门店ID（可选）
     * @return 门店商品简单列表
     */
    @GetMapping("/simple-list")
    @Operation(summary = "获得门店商品简单列表")
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<List<StoreProductSimpleRespVO>> getStoreProductSimpleList(
            @RequestParam(value = "storeId", required = false) String storeId) {
        if (StrUtil.isNotBlank(storeId)) {
            return success(storeProductService.getStoreProductSimpleList(storeId));
        }
        return success(storeProductService.getAllStoreProductSimpleList());
    }

    /**
     * 导出门店商品 Excel
     *
     * 【What】导出门店商品数据为 Excel 文件
     *
     * @param pageReqVO 分页查询条件
     * @param response HTTP响应
     */
    @GetMapping("/export")
    @Operation(summary = "导出门店商品 Excel")
    @PreAuthorize("@ss.hasPermission('business:store-product:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStoreProductExcel(@Valid StoreProductPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StoreProductRespVO> list = storeProductService.getStoreProductPage(pageReqVO).getList();
        ExcelUtils.write(response, "门店商品.xls", "数据", StoreProductRespVO.class, list);
    }

    /**
     * 获得导入门店商品模板
     *
     * 【What】下载门店商品导入 Excel/CSV 模板
     *
     * @param format 模板格式：excel 或 csv
     * @param response HTTP响应
     */
    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入门店商品模板")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "format", description = "模板格式：excel 或 csv", example = "excel")
    })
    public void importTemplate(@RequestParam(value = "format", required = false, defaultValue = "excel") String format,
                               HttpServletResponse response) throws IOException {
        List<StoreProductImportExcelVO> list = Arrays.asList(
                StoreProductImportExcelVO.builder()
                        .storeId("S001")
                        .productSkuId("SKU001")
                        .storeProductOwnership("入店")
                        .storeProductPosStatus("正常")
                        .storeProductPrice(new java.math.BigDecimal("99.99"))
                        .storeProductIsActive(1)
                        .build()
        );
        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=门店商品导入模板.csv");
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("门店ID,SKU ID,归属,POS状态,价格,是否启用\n");
            for (StoreProductImportExcelVO item : list) {
                csvContent.append(escapeCsv(item.getStoreId())).append(",")
                        .append(escapeCsv(item.getProductSkuId())).append(",")
                        .append(escapeCsv(item.getStoreProductOwnership())).append(",")
                        .append(escapeCsv(item.getStoreProductPosStatus())).append(",")
                        .append(item.getStoreProductPrice()).append(",")
                        .append(item.getStoreProductIsActive()).append("\n");
            }
            response.getWriter().write("\uFEFF" + csvContent.toString());
        } else {
            ExcelUtils.write(response, "门店商品导入模板.xls", "门店商品列表", StoreProductImportExcelVO.class, list);
        }
    }

    /**
     * CSV字段转义
     *
     * 【What】处理CSV特殊字符
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 导入门店商品
     *
     * 【What】批量导入门店商品数据
     *
     * 【Constraints】
     * - isUpdateSupport=true 时，已存在商品会更新
     * - isUpdateSupport=false 时，已存在商品会报错
     *
     * 【Pitfalls】
     * - 【陷阱】只校验了入店归属的唯一性
     *
     * @param file Excel文件
     * @param updateSupport 是否支持更新
     * @return 导入结果
     */
    @PostMapping("/import")
    @Operation(summary = "导入门店商品")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "file", description = "Excel 文件", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('business:store-product:import')")
    public CommonResult<StoreProductImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<StoreProductImportExcelVO> list = ExcelUtils.read(file, StoreProductImportExcelVO.class);
        return success(storeProductService.importStoreProductList(list, updateSupport));
    }

    /**
     * 获得门店库存
     *
     * 【What】根据门店商品ID查询库存信息
     *
     * @param storeProductId 门店商品ID
     * @return 门店库存信息
     */
    @GetMapping("/get-by-store-product/{storeProductId}")
    @Operation(summary = "获得门店库存")
    @Parameter(name = "storeProductId", description = "门店商品ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<StoreStockRespVO> getStoreStockByStoreProductId(@PathVariable("storeProductId") String storeProductId) {
        return success(storeProductService.getStoreStockByStoreProductId(storeProductId));
    }

    /**
     * 获得SKU简单列表
     *
     * 【What】获取所有 SKU 的简化信息
     *
     * 【Constraints】
     * - 无权限控制，用于公共查询
     *
     * @return SKU简单列表
     */
    @GetMapping("/store/product/simple-list")
    @Operation(summary = "获得SKU简单列表")
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<List<SkuSimpleRespVO>> getSkuSimpleList() {
        return success(storeProductService.getSkuSimpleList());
    }

}
