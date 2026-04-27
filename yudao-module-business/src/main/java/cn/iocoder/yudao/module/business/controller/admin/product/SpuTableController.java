package cn.iocoder.yudao.module.business.controller.admin.product;

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
import java.math.BigDecimal;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.service.product.SpuTableService;
import org.springframework.web.multipart.MultipartFile;

/**
 * SPU（标准产品单位）基础分类管理控制器
 *
 * 本控制器负责管理后台系统中SPU基础分类的RESTful API接口。
 *
 * **业务说明：**
 * - SPU（Standard Product Unit）是商品信息聚合的最小单位，是一组标准化信息的集合
 * - 该控制器关联管理SKU（库存量单位）和UPC（通用产品码）子表数据
 * - 支持SPU/SKU/UPC的联合导入功能，实现批量数据维护
 *
 * **模块职责：**
 * 1. SPU基础分类的增删改查操作
 * 2. SKU商品主数据的查询管理
 * 3. SPU/SKU/UPC数据的Excel导入导出
 * 4. 导入模板的下载功能
 *
 * **设计思路：**
 * - 采用VO对象进行请求和响应数据封装，确保API接口的稳定性
 *   原因：DO（数据对象）直接暴露会破坏分层架构，且数据库字段变更会影响前端
 * - 使用BeanUtils进行DO与VO之间的对象转换，保持分层清晰
 *   原因：手动setter转换容易遗漏字段，BeanUtils自动映射更可靠且易维护
 * - 结合Swagger注解提供API文档支持，减少前后端沟通成本
 *   原因：@Operation注解让接口文档与代码同步更新，避免文档落后于实现
 * - 集成权限控制，确保接口安全访问
 *   原因：通过@PreAuthorize注解在方法级别控制，比在Service层控制更精细
 *
 */
@Tag(name = "管理后台 - SPU基础分类")
@RestController
@RequestMapping("/business/spu-table")
@Validated
public class SpuTableController {

    @Resource
    private SpuTableService spuTableService;

    /**
     * 创建新的SPU基础分类
     *
     * @param createReqVO SPU创建请求对象，包含SPU编码、名称、品牌、产地、厂商等基本信息
     * @return 新创建的SPU记录ID，用于后续关联操作
     * @throws IllegalArgumentException 当必填字段缺失或格式不正确时
     * @throws IllegalStateException 当SPU编码已存在时
     *
     * 【设计原因】
     * - 返回新创建的ID而非Boolean，因为创建操作后前端通常需要对新记录进行后续操作
     *   （如跳转到详情页、关联其他数据），返回ID可以减少前端再次查询的开销
     */
    @PostMapping("/create")
    @Operation(summary = "创建SPU基础分类")
    @PreAuthorize("@ss.hasPermission('business:spu-table:create')")
    public CommonResult<Long> createSpuTable(@Valid @RequestBody SpuTableSaveReqVO createReqVO) {
        return success(spuTableService.createSpuTable(createReqVO));
    }

    /**
     * 更新已有的SPU基础分类信息
     *
     * @param updateReqVO SPU更新请求对象，包含SPU编号及需要更新的字段
     * @return 更新操作是否成功
     * @throws IllegalArgumentException 当SPU编号不存在或必填字段验证失败时
     * @throws IllegalStateException 当更新的数据存在业务冲突时
     */
    @PutMapping("/update")
    @Operation(summary = "更新SPU基础分类")
    @PreAuthorize("@ss.hasPermission('business:spu-table:update')")
    public CommonResult<Boolean> updateSpuTable(@Valid @RequestBody SpuTableSaveReqVO updateReqVO) {
        spuTableService.updateSpuTable(updateReqVO);
        return success(true);
    }

    /**
     * 根据ID删除单个SPU基础分类
     *
     * @param productSpuId SPU记录的业务编号，用于定位要删除的记录
     * @return 删除操作是否成功
     * @throws IllegalArgumentException 当SPU编号不存在时
     * @throws IllegalStateException 当SPU存在关联的SKU数据无法删除时
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除SPU基础分类")
    @Parameter(name = "productSpuId", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:spu-table:delete')")
    public CommonResult<Boolean> deleteSpuTable(@RequestParam("productSpuId") Long productSpuId) {
        spuTableService.deleteSpuTable(productSpuId);
        return success(true);
    }

    /**
     * 根据ID列表批量删除SPU基础分类
     *
     * @param ids SPU记录的ID列表，支持批量删除操作以提高效率
     * @return 批量删除操作是否成功
     * @throws IllegalArgumentException 当存在无效的ID时
     * @throws IllegalStateException 当部分SPU存在关联数据无法删除时
     *
     * 【设计原因】
     * - 提供批量删除接口，因为删除操作在后台管理中通常是高频操作，
     *   单个删除需要多次HTTP请求，批量删除显著提升操作效率
     * - 批量删除失败时整体回滚，因为部分删除成功部分失败会导致数据不一致
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除SPU基础分类")
                @PreAuthorize("@ss.hasPermission('business:spu-table:delete')")
    public CommonResult<Boolean> deleteSpuTableList(@RequestParam("ids") List<Long> ids) {
        spuTableService.deleteSpuTableListByIds(ids);
        return success(true);
    }

    /**
     * 根据ID获取SPU基础分类详情
     *
     * 该接口会同时返回SPU基本信息及其关联的所有SKU数据，实现一次查询获取完整商品信息。
     * SKU列表按照商品款式维度进行聚合，便于前端展示商品规格选择。
     *
     * 【设计原因】
     * - 采用嵌套返回SKU列表而非单独接口返回，因为商品详情页通常需要同时展示SPU和SKU信息
     *   避免前端因两次请求导致的竞态条件，也减少HTTP开销
     * - 如果SPU不存在则返回null而非抛异常，因为查询单个资源不存在是正常业务场景
     *
     * @param productSpuId SPU记录的业务编号
     * @return SPU详细信息及其关联的SKU列表，如果SPU不存在则返回null
     * @throws IllegalArgumentException 当SPU编号格式不正确时
     */
    @GetMapping("/get")
    @Operation(summary = "获得SPU基础分类")
    @Parameter(name = "productSpuId", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<SpuTableRespVO> getSpuTable(@RequestParam("productSpuId") Long productSpuId) {
        SpuTableDO spuTable = spuTableService.getSpuTable(productSpuId);
        SpuTableRespVO respVO = BeanUtils.toBean(spuTable, SpuTableRespVO.class);
        if (respVO != null) {
            // 查询关联的SKU列表并填充到响应对象中
            List<SkuTableDO> skuList = spuTableService.getSkuTableListByProductSpuId(productSpuId);
            respVO.setSkuTables(BeanUtils.toBean(skuList, SkuTableRespVO.class));
        }
        return success(respVO);
    }

    /**
     * 分页查询SPU基础分类列表
     *
     * 支持按SPU编码、名称、品牌等条件进行筛选，返回分页结果以应对大数据量场景。
     * 分页参数通过SpuTablePageReqVO传递，包含页码、每页大小及排序规则。
     *
     * @param pageReqVO 分页查询请求对象，包含分页参数和筛选条件
     * @return 分页后的SPU列表，每页数据量由pageReqVO.pageSize控制
     * @throws IllegalArgumentException 当分页参数不合法时
     */
    @GetMapping("/page")
    @Operation(summary = "获得SPU基础分类分页")
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<PageResult<SpuTableRespVO>> getSpuTablePage(@Valid SpuTablePageReqVO pageReqVO) {
        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SpuTableRespVO.class));
    }

    @GetMapping("/page-aggregate")
    @Operation(summary = "获得SPU基础分类聚合分页")
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<PageResult<SpuTableAggregateRespVO>> getSpuTableAggregatePage(@Valid SpuTablePageReqVO pageReqVO) {
        return success(spuTableService.getSpuTableAggregatePage(pageReqVO));
    }

    /**
     * 导出SPU基础分类数据到Excel
     *
     * 该接口会将所有符合条件的SPU数据导出为Excel文件，支持大数据量导出。
     * 导出时设置PAGE_SIZE_NONE以获取完整数据集，实际项目中建议根据数据量评估是否需要异步处理。
     *
     * 【设计原因】
     * - 使用同步导出而非异步任务，因为：数据量可控时同步导出用户体验更好（立即下载），
     *   异步任务会增加系统复杂度（状态查询、失败重试），且本接口已有筛选条件限制数据范围
     * - 导出接口使用GET而非POST，因为HTTP语义上GET用于获取资源，导出本质也是获取资源
     *
     * @param pageReqVO 导出数据的筛选条件，若为空则导出全部数据
     * @param response HTTP响应对象，用于文件流输出
     * @throws IOException 当文件写入失败或响应流被占用时
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出SPU基础分类 Excel")
    @PreAuthorize("@ss.hasPermission('business:spu-table:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSpuTableExcel(@Valid SpuTablePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        List<SpuSkuUpcExportVO> list = spuTableService.getExportData(pageReqVO);
        ExcelUtils.write(response, "SPU_SKU_UPC商品数据导出.xls", "数据", SpuSkuUpcExportVO.class, list);
    }

    // ==================== 子表（SKU商品主数据） ====================

    /**
     * 根据SPU ID查询关联的SKU商品主数据列表
     *
     * SKU是SPU的具体销售单元，一个SPU可以对应多个SKU（不同颜色、尺寸等）。
     * 该接口用于获取某SPU下的所有可用款式规格。
     *
     * 【设计原因】
     * - 提供独立的SKU查询接口，因为某些业务场景（如购物车、订单）只需查询SKU信息，
     *   不需要加载完整的SPU数据，可以提升系统性能
     * - 使用List返回而非分页，因为单个SPU下的SKU数量通常有限（一般不超过几十个），
     *   不需要分页带来的额外开销
     *
     * @param productSpuId SPU记录的业务编号，用于筛选关联的SKU数据
     * @return 该SPU下的所有SKU列表，若无关联则返回空列表
     * @throws IllegalArgumentException 当SPU编号格式不正确时
     */
    @GetMapping("/sku-table/list-by-product-spu-id")
    @Operation(summary = "获得SKU商品主数据列表")
    @Parameter(name = "productSpuId", description = "所属SPU")
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<List<SkuTableDO>> getSkuTableListByProductSpuId(@RequestParam("productSpuId") Long productSpuId) {
        return success(spuTableService.getSkuTableListByProductSpuId(productSpuId));
    }

    // ==================== 导入功能 ====================

    /**
     * 下载SPU/SKU/UPC联合导入模板
     *
     * 该模板展示了SPU、SKU、UPC三层次数据的导入格式及填写规范。
     * 模板中包含5行示例数据，演示了：
     * - 同一SPU下多个SKU的关联关系
     * - 一个SKU可对应多个UPC码（主码/副码）
     * - 不同UPC类型的编码规则（EAN-13、UPC-A、CODE128）
     *
     * @param response HTTP响应对象，用于文件流输出
     * @throws IOException 当模板文件写入失败时
     */
    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入SPU/SKU/UPC模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 构建示例数据集，包含SPU、SKU、UPC三个维度的完整示例
        List<SpuSkuUpcImportVO> list = Arrays.asList(
            // 第一行：SPU + SKU1 + UPC1（白色款主码）
            SpuSkuUpcImportVO.builder()
                .productSpuCode("SPU001")
                .productSpuName("示例商品")
                .productBrand("示例品牌")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("示例厂商")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .productSkuCode("SKU001-白")
                .productSkuName("白色款式")
                .productSkuEan("1234567890123")
                .productWeight(new BigDecimal("100"))
                .productWeightUnit("g")
                .productLength(new BigDecimal("10"))
                .productWidth(new BigDecimal("10"))
                .productHeight(new BigDecimal("10"))
                .productCostPrice(new BigDecimal("50"))
                .productRetailPrice(new BigDecimal("100"))
                .productSkuStatus(1)
                .productUpcType("EAN-13")
                .productUpcValue("1234567890123")
                .productUpcIsPrimary(1)
                .productUpcStatus(1)
                .build(),
            // 第二行：同一个SPU + SKU1 + UPC2（白色款副码，用于不同包装规格）
            SpuSkuUpcImportVO.builder()
                .productSpuCode("SPU001")
                .productSpuName("示例商品")
                .productBrand("示例品牌")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("示例厂商")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .productSkuCode("SKU001-白")
                .productSkuName("白色款式")
                .productSkuEan("1234567890123")
                .productWeight(new BigDecimal("100"))
                .productWeightUnit("g")
                .productLength(new BigDecimal("10"))
                .productWidth(new BigDecimal("10"))
                .productHeight(new BigDecimal("10"))
                .productCostPrice(new BigDecimal("50"))
                .productRetailPrice(new BigDecimal("100"))
                .productSkuStatus(1)
                .productUpcType("UPC-A")
                .productUpcValue("012345678905")
                .productUpcIsPrimary(0)
                .productUpcStatus(1)
                .build(),
            // 第三行：同一个SPU + SKU2 + UPC1（黑色款主码）
            SpuSkuUpcImportVO.builder()
                .productSpuCode("SPU001")
                .productSpuName("示例商品")
                .productBrand("示例品牌")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("示例厂商")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .productSkuCode("SKU001-黑")
                .productSkuName("黑色款式")
                .productSkuEan("1234567890124")
                .productWeight(new BigDecimal("100"))
                .productWeightUnit("g")
                .productLength(new BigDecimal("10"))
                .productWidth(new BigDecimal("10"))
                .productHeight(new BigDecimal("10"))
                .productCostPrice(new BigDecimal("50"))
                .productRetailPrice(new BigDecimal("100"))
                .productSkuStatus(1)
                .productUpcType("EAN-13")
                .productUpcValue("1234567890124")
                .productUpcIsPrimary(1)
                .productUpcStatus(1)
                .build(),
            // 第四行：同一个SPU + SKU2 + UPC2（黑色款副码，使用CODE128类型）
            SpuSkuUpcImportVO.builder()
                .productSpuCode("SPU001")
                .productSpuName("示例商品")
                .productBrand("示例品牌")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("示例厂商")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .productSkuCode("SKU001-黑")
                .productSkuName("黑色款式")
                .productSkuEan("1234567890124")
                .productWeight(new BigDecimal("100"))
                .productWeightUnit("g")
                .productLength(new BigDecimal("10"))
                .productWidth(new BigDecimal("10"))
                .productHeight(new BigDecimal("10"))
                .productCostPrice(new BigDecimal("50"))
                .productRetailPrice(new BigDecimal("100"))
                .productSkuStatus(1)
                .productUpcType("CODE128")
                .productUpcValue("SKU001-BLACK-001")
                .productUpcIsPrimary(0)
                .productUpcStatus(1)
                .build(),
            // 第五行：新SPU + SKU1（另一个商品系列的示例）
            SpuSkuUpcImportVO.builder()
                .productSpuCode("SPU002")
                .productSpuName("示例商品2")
                .productBrand("示例品牌2")
                .categoryId(2L)
                .productOrigin("美国")
                .productManufacturer("示例厂商2")
                .productSpecTemplate("标准规格2")
                .productSpuStatus(1)
                .productSkuCode("SKU002-001")
                .productSkuName("标准款式")
                .productSkuEan("9876543210987")
                .productWeight(new BigDecimal("200"))
                .productWeightUnit("g")
                .productLength(new BigDecimal("15"))
                .productWidth(new BigDecimal("15"))
                .productHeight(new BigDecimal("15"))
                .productCostPrice(new BigDecimal("80"))
                .productRetailPrice(new BigDecimal("150"))
                .productSkuStatus(1)
                .productUpcType("EAN-13")
                .productUpcValue("9876543210987")
                .productUpcIsPrimary(1)
                .productUpcStatus(1)
                .build()
        );
        // 生成包含示例数据的Excel模板文件
        ExcelUtils.write(response, "SPU_SKU_UPC导入模板.xls", "导入数据", SpuSkuUpcImportVO.class, list);
    }

    /**
     * 导入SPU/SKU/UPC数据
     *
     * 该接口支持批量导入商品主数据，包含SPU基本信息、SKU销售单元及UPC条码。
     * 数据关联逻辑：通过SPU编码和SKU编码进行关联，支持同一SPU下多个SKU的批量维护。
     *
     * 导入模式：
     * - 新增模式（默认）：遇到重复数据时报错终止
     * - 更新模式（updateSupport=true）：对已存在的数据进行覆盖更新
     *
     * 【设计原因】
     * - 将SPU/SKU/UPC三层数据合并为一行导入，因为它们存在强业务关联，
     *   拆分多个接口导入会增加数据不一致风险，且用户体验不佳
     * - updateSupport默认为false，因为数据安全优先于操作便捷性，
     *   防止用户误操作覆盖已有数据，且系统通常有专门的数据修正流程
     * - 导入接口使用POST而非GET，因为文件上传涉及请求体，不适合GET语义
     *
     * @param file 导入的Excel文件，包含SPU、SKU、UPC数据
     * @param updateSupport 是否支持更新已有数据，默认为false（新增模式）
     * @return 导入结果，包含成功条数、失败条数及错误详情
     * @throws Exception 当文件格式错误或解析失败时
     */
    @PostMapping("/import")
    @Operation(summary = "导入SPU/SKU/UPC数据")
    @io.swagger.v3.oas.annotations.Parameters({
        @io.swagger.v3.oas.annotations.Parameter(name = "file", description = "Excel 文件", required = true),
        @io.swagger.v3.oas.annotations.Parameter(name = "updateSupport", description = "是否支持更新", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('business:spu-table:import')")
    public CommonResult<SpuImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        // 使用ExcelUtils读取上传的Excel文件并转换为对象列表
        List<SpuSkuUpcImportVO> list = ExcelUtils.read(file, SpuSkuUpcImportVO.class);
        // 调用服务层处理导入逻辑，返回导入结果统计
        return success(spuTableService.importSpuSkuUpcList(list, updateSupport));
    }

}