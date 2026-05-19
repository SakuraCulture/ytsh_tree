package cn.iocoder.yudao.module.business.controller.admin.store;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.annotation.security.PermitAll;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.hutool.core.util.StrUtil;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;

import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.framework.ip.core.utils.AreaUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 门店管理接口
 *
 * ==============================================================
 * 【Why - 为什么要做这个模块】
 * ==============================================================
 * 门店是整个业务系统的核心实体之一，向上对接平台（如美团、饿了么），
 * 向下管理商品和订单。本模块采用主表+子表的设计模式：
 *
 * 设计选择：主表+多子表 vs 单表扁平化
 * - 选择主表+子表的原因：
 *   1. 门店基础信息（名称、地址）变更频繁，但子表（空间、加盟商）相对稳定
 *   2. 不同子表属于不同的业务域（运营、财务、人力），解耦后便于权限控制
 *   3. 单表扁平化会导致字段过多（当前已达 50+），难以维护
 * - 不选择微服务拆分的考虑：门店数据是强一致性场景，拆分会增加分布式事务复杂度
 *
 * ==============================================================
 * 【What - 这个模块做什么】
 * ==============================================================
 * - 门店主档的 CRUD 操作
 * - 门店子表（空间、归属、经营状态等）的查询
 * - 门店与平台关联关系管理
 * - 门店数据导入导出
 *
 * ==============================================================
 * 【Constraints - 业务约束】
 * ==============================================================
 * - 门店编码（storeId）全局唯一，不允许重复
 * - 门店名称全局唯一，用于防重校验
 * - 删除门店时必须级联删除所有子表数据（数据完整性约束）
 * - 门店状态变更后必须请求刷新门店平台缓存；存在事务时在提交后执行
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【陷阱1】子表为空时仍插入记录 → 修复：判断 null 后才插入
 * - 【陷阱2】并发导入导致重复 storeId → 修复：事务隔离级别 READ_COMMITTED
 * - 【陷阱3】删除时未清理 Redis 缓存 → 教训：必须在事务后同步缓存
 * - 【陷阱4】updateTime 不更新 → 教训：需调用 .clean() 方法
 *
 * @author SMK
 * @see StoreService
 */
@Tag(name = "管理后台 - 门店")
@RestController
@RequestMapping("/business/table")
@Validated
    public class StoreController {

    @Resource
    private StoreService storeService;

    /**
     * 创建门店
     *
     * 【What】
     * 在事务中执行以下操作：
     * 1. 校验 storeId、storeName 不重复
     * 2. 插入主表 store_table
     * 3. 按需插入子表
     * 4. 请求刷新门店平台缓存；存在事务时在提交后执行
     *
     * 【Constraints】
     * - storeId 由前端生成，需校验格式
     * - storeName 全局唯一
     *
     * 【Pitfalls】
     * - 【教训2024-03】storeId 格式校验缺失曾导致非法ID入库
     *
     * @param createReqVO 创建信息
     * @return 门店ID
     */
    @PostMapping("/create")
    @Operation(summary = "创建门店")
    @PreAuthorize("@ss.hasPermission('business:table:create')")
    public CommonResult<String> createStore(@Valid @RequestBody StoreSaveReqVO createReqVO) {
        return success(storeService.createStore(createReqVO));
    }

    /**
     * 更新门店
     *
     * 【What】
     * 1. 校验门店存在性
     * 2. 检查 storeId、storeName 是否被其他门店占用
     * 3. 更新主表和子表
     * 4. 请求刷新门店平台缓存；存在事务时在提交后执行
     *
     * 【Constraints】
     * - 子表更新使用 insertOrUpdate，依赖 .clean() 方法
     *
     * 【Pitfalls】
     * - 【教训2024-03】未调用 .clean() 导致 updateTime 未更新
     *
     * @param updateReqVO 更新信息
     * @return 成功标志
     */
    @PutMapping("/update")
    @Operation(summary = "更新门店")
    @PreAuthorize("@ss.hasPermission('business:table:update')")
    public CommonResult<Boolean> updateStore(@Valid @RequestBody StoreSaveReqVO updateReqVO) {
        storeService.updateStore(updateReqVO);
        return success(true);
    }

    /**
     * 删除门店
     *
     * 【What】
     * 1. 校验门店存在性
     * 2. 删除主表记录
     * 3. 级联删除所有子表
     * 4. 请求刷新门店平台缓存；存在事务时在提交后执行
     *
     * 【Constraints】
     * - 必须级联删除子表，否则产生孤儿数据
     *
     * 【Pitfalls】
     * - 【教训2024-03】未级联删除子表导致统计数据不一致
     *
     * @param id 门店ID
     * @return 成功标志
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除门店")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:table:delete')")
    public CommonResult<Boolean> deleteStore(@RequestParam("id") String id) {
        storeService.deleteStore(id);
        return success(true);
    }

    /**
     * 批量删除门店
     *
     * 【What】批量删除主表和所有子表，并请求刷新门店平台缓存；存在事务时在提交后执行
     *
     * 【Constraints】
     * - ids 列表不宜过大，建议分批处理
     *
     * @param ids 门店ID列表
     * @return 成功标志
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除门店")
    @PreAuthorize("@ss.hasPermission('business:table:delete')")
    public CommonResult<Boolean> deleteStoreList(@RequestParam("ids") List<String> ids) {
        storeService.deleteStoreListByIds(ids);
        return success(true);
    }

    /**
     * 获得门店
     *
     * 【What】根据 storeId 查询主表记录
     *
     * @param id 门店ID
     * @return 门店信息
     */
    @GetMapping("/get")
    @Operation(summary = "获得门店")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<StoreRespVO> getStore(@RequestParam("id") String id) {
        StoreDO store = storeService.getStore(id);
        StoreRespVO respVO = BeanUtils.toBean(store, StoreRespVO.class);
        convertRegionName(respVO, store.getRegionCode());
        return success(respVO);
    }

    /**
     * 获得门店分页
     *
     * 【What】支持分页、排序、关键词搜索的门店列表查询
     *
     * @param pageReqVO 分页查询
     * @return 门店分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "获得门店分页")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<PageResult<StoreRespVO>> getStorePage(@Valid StorePageReqVO pageReqVO) {
        PageResult<StoreDO> pageResult = storeService.getStorePage(pageReqVO);
        List<StoreRespVO> respVOList = BeanUtils.toBean(pageResult.getList(), StoreRespVO.class);
        for (int i = 0; i < respVOList.size(); i++) {
            convertRegionName(respVOList.get(i), pageResult.getList().get(i).getRegionCode());
        }
        return success(new PageResult<>(respVOList, pageResult.getTotal()));
    }

    /**
     * 获得门店简单列表（用于搜索建议）
     *
     * 【What】返回门店简化信息，用于下拉选择、搜索建议等轻量场景
     *
     * @param keyword 关键词（匹配 storeId 或 storeName）
     * @return 门店列表
     */
    @GetMapping("/list-simple")
    @Operation(summary = "获得门店简单列表（用于搜索建议）")
    @PreAuthorize("@ss.hasAnyPermissions('business:table:query', 'business:store-product:query')")
    public CommonResult<List<StoreRespVO>> getStoreSimpleList(@RequestParam(value = "keyword", required = false) String keyword) {
        StorePageReqVO reqVO = new StorePageReqVO();
        if (StrUtil.isNotBlank(keyword)) {
            reqVO.setStoreId(keyword);
            reqVO.setStoreName(keyword);
        }
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        PageResult<StoreDO> pageResult = storeService.getStorePage(reqVO);
        return success(BeanUtils.toBean(pageResult.getList(), StoreRespVO.class));
    }

    /**
     * 获取所有门店简单信息列表
     *
     * 【What】返回所有门店的简化信息列表，用于下拉选择等场景
     *
     * @param platformId 平台ID（可选，用于过滤特定平台的门店）
     * @return 门店简单信息列表
     */
    @GetMapping("/list-all-simple")
    @Operation(summary = "获取所有门店简单信息列表")
    @PreAuthorize("@ss.hasAnyPermissions('business:table:query', 'business:store-product:query')")
    public CommonResult<List<StoreSimpleRespVO>> getStoreAllSimpleList(@RequestParam(value = "platformId", required = false) Long platformId) {
        return success(storeService.getAllSimpleList(platformId));
    }

    /**
     * 导出门店 Excel
     *
     * 【What】导出门店数据为 Excel 文件
     *
     * @param pageReqVO 分页查询条件
     * @param response HTTP响应
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出门店 Excel")
    @PreAuthorize("@ss.hasPermission('business:table:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStoreExcel(@Valid StorePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        List<StoreImportExcelVO> list = storeService.getStoreImportExcelList(pageReqVO);
        ExcelUtils.write(response, "门店.xls", "数据", StoreImportExcelVO.class, list);
    }

    // ==================== 子表（门店空间） ====================

    /**
     * 获得门店空间
     *
     * 【What】查询门店的空间信息
     *
     * @param storeId 门店ID
     * @return 门店空间信息
     */
    @GetMapping("/space-table/get-by-store-id")
    @Operation(summary = "获得门店空间")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<SpaceTableDO> getSpaceTableByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getSpaceTableByStoreId(storeId));
    }

    // ==================== 子表（门店架构归属） ====================

    /**
     * 获得门店架构归属
     *
     * 【What】查询门店的组织归属信息
     *
     * @param storeId 门店ID
     * @return 门店架构归属信息
     */
    @GetMapping("/affiliation-table/get-by-store-id")
    @Operation(summary = "获得门店架构归属")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<AffiliationTableDO> getAffiliationTableByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getAffiliationTableByStoreId(storeId));
    }

    // ==================== 子表（门店经营状态） ====================

    /**
     * 获得门店经营状态
     *
     * 【What】查询门店的经营状态信息
     *
     * @param storeId 门店ID
     * @return 门店经营状态信息
     */
    @GetMapping("/status-table/get-by-store-id")
    @Operation(summary = "获得门店经营状态")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<BusinessStatusTableDO> getStatusTableByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getStatusTableByStoreId(storeId));
    }

    // ==================== 子表（门店加盟商信息） ====================

    /**
     * 获得门店加盟商信息
     *
     * 【What】查询门店的加盟商信息
     *
     * @param storeId 门店ID
     * @return 门店加盟商信息
     */
    @GetMapping("/franchisee-table/get-by-store-id")
    @Operation(summary = "获得门店加盟商信息")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<FranchiseeTableDO> getFranchiseeTableByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getFranchiseeTableByStoreId(storeId));
    }

    // ==================== 子表（门店联系人通讯录） ====================

    /**
     * 获得门店联系人通讯录列表
     *
     * 【What】查询门店的所有联系人信息
     *
     * 【Pitfalls】
     * - 【教训2024-04】联系人 ID 频繁变化导致第三方关联失效
     *
     * @param storeId 门店ID
     * @return 门店联系人列表
     */
    @GetMapping("/contact-table/list-by-store-id")
    @Operation(summary = "获得门店联系人通讯录列表")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<List<ContactTableDO>> getContactTableListByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getContactTableListByStoreId(storeId));
    }

    // ==================== 子表（门店平台关联） ====================

    /**
     * 获得门店平台关联列表
     *
     * 【What】查询门店与各平台的关联信息
     *
     * @param storeId 门店ID
     * @return 门店平台关联列表
     */
    @GetMapping("/platform-table/list-by-store-id")
    @Operation(summary = "获得门店平台关联列表")
    @Parameter(name = "storeId", description = "门店ID")
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<List<StorePlatformRespVO>> getPlatformTableListByStoreId(@RequestParam("storeId") String storeId) {
        return success(storeService.getPlatformTableListByStoreId(storeId));
    }

    @GetMapping("/supply-line/get-by-store-id")
    @Operation(summary = "获得门店供货与线路汇总")
    @Parameter(name = "storeId", description = "门店ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:table:query')")
    public CommonResult<StoreSupplyLineRespVO> getStoreSupplyLineSummary(@RequestParam("storeId") String storeId) {
        return success(storeService.getStoreSupplyLineSummary(storeId));
    }

    /**
     * 按平台搜索门店简表。
     */
    @GetMapping("/platform-info/search-simple")
    @Operation(summary = "按平台搜索门店简表")
    @PreAuthorize("@ss.hasAnyPermissions('business:table:query', 'business:store-product:query')")
    public CommonResult<List<StoreSimpleRespVO>> searchPlatformStoreSimpleList(@RequestParam("platformId") Long platformId,
                                                                                @RequestParam(value = "keyword", required = false) String keyword,
                                                                                @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                                                                                @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return success(storeService.searchPlatformStoreSimpleList(platformId, keyword, pageNo, pageSize));
    }

    /**
     * 按平台和平台门店ID批量获取门店简表。
     */
    @PostMapping("/platform-info/list-simple")
    @Operation(summary = "按平台和平台门店ID批量获取门店简表")
    @PreAuthorize("@ss.hasAnyPermissions('business:table:query', 'business:store-product:query')")
    public CommonResult<List<StoreSimpleRespVO>> getPlatformStoreSimpleList(@Valid @RequestBody StorePlatformSimpleBatchReqVO reqVO) {
        return success(storeService.getPlatformStoreSimpleList(reqVO.getPlatformId(), reqVO.getPlatformStoreIds()));
    }

    /**
     * 获取已开店门店的平台信息列表(从Redis获取)
     *
     * 【What】从 Redis 缓存获取已开店的门店平台关联信息
     *
     * 【Why - 为什么要用 Redis 缓存？】
     * - 高频读取：定时同步任务每分钟调用，数据库压力大
     * - 查询优化：避免每次都 JOIN 查询
     *
     * @return 门店平台信息列表
     */
    @GetMapping("/platform-info/list")
    @Operation(summary = "获取全部门店的平台信息列表(从Redis获取)")
    @PermitAll
    public CommonResult<List<StorePlatformInfoRespVO>> getStorePlatformInfoList() {
        return success(storeService.getStorePlatformInfoList());
    }

    /**
     * 手动同步门店平台信息到 Redis
     *
     * 【What】手动触发门店平台缓存同步，保持同步执行语义
     *
     * 【Why - 为什么要手动同步？】
     * - 缓存变更可追踪：每次门店增删改后主动刷新
     * - 避免依赖 TTL 过期导致的数据不一致
     *
     * @return 成功标志
     */
    @GetMapping("/platform-info/sync")
    @Operation(summary = "手动同步门店平台信息到 Redis")
    @PermitAll
    public CommonResult<Boolean> syncStorePlatformInfo() {
        storeService.syncStorePlatformInfo();
        return success(true);
    }

    // ==================== 导入功能 ====================

    /**
     * 获得导入门店模板
     *
     * 【What】下载门店导入 Excel 模板
     *
     * @param format 模板格式：excel 或 csv
     * @param response HTTP响应
     */
    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入门店模板")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "format", description = "模板格式：excel 或 csv", example = "excel")
    })
    public void importTemplate(@RequestParam(value = "format", required = false, defaultValue = "excel") String format,
                               HttpServletResponse response) throws IOException {
        List<StoreImportExcelVO> list = Arrays.asList(
                StoreImportExcelVO.builder()
                        .storeId("S001")
                        .storeName("示例门店")
                        .regionCode("110000")
                        .address("示例地址")
                        .area("EAST")
                        .storeStatus(1)
                        .buildingArea(new java.math.BigDecimal("120.00"))
                        .coldStorageArea(new java.math.BigDecimal("0.00"))
                        .businessMode("DIRECT")
                        .storeType("O2O")
                        .currentStatus("NORMAL")
                        .openDate(java.time.LocalDate.of(2026, 4, 1))
                        .signDate(java.time.LocalDate.of(2026, 3, 15))
                        .franchiseeName("示例加盟商")
                        .franchiseePhone("13800000000")
                        .franchiseeFee(new java.math.BigDecimal("10000.00"))
                        .securityDeposit(new java.math.BigDecimal("5000.00"))
                        .contractStart(java.time.LocalDate.of(2026, 5, 1))
                        .contractEnd(java.time.LocalDate.of(2027, 4, 30))
                        .build()
        );
        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=门店导入模板.csv");
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("门店编码,门店名称,行政区划代码,详细地址,门店区域,状态(0停用1正常),房屋面积(㎡),冷库面积(㎡),经营方式,门店类型,当前状态,开业日期,签约日期,加盟商名称,加盟联系方式,加盟费,保证金,合同开始日期,合同结束日期\n");
            for (StoreImportExcelVO item : list) {
                csvContent.append(escapeCsv(item.getStoreId())).append(",")
                        .append(escapeCsv(item.getStoreName())).append(",")
                        .append(escapeCsv(item.getRegionCode())).append(",")
                        .append(escapeCsv(item.getAddress())).append(",")
                        .append(escapeCsv(item.getArea())).append(",")
                        .append(item.getStoreStatus()).append(",")
                        .append(item.getBuildingArea()).append(",")
                        .append(item.getColdStorageArea()).append(",")
                        .append(escapeCsv(item.getBusinessMode())).append(",")
                        .append(escapeCsv(item.getStoreType())).append(",")
                        .append(escapeCsv(item.getCurrentStatus())).append(",")
                        .append(item.getOpenDate()).append(",")
                        .append(item.getSignDate()).append(",")
                        .append(escapeCsv(item.getFranchiseeName())).append(",")
                        .append(escapeCsv(item.getFranchiseePhone())).append(",")
                        .append(item.getFranchiseeFee()).append(",")
                        .append(item.getSecurityDeposit()).append(",")
                        .append(item.getContractStart()).append(",")
                        .append(item.getContractEnd()).append("\n");
            }
            response.getWriter().write("\uFEFF" + csvContent.toString());
        } else {
            ExcelUtils.write(response, "门店导入模板.xls", "门店列表", StoreImportExcelVO.class, list);
        }
    }

    /**
     * 导入门店
     *
     * 【What】批量导入门店数据，支持新增和更新两种模式
     *
     * 【Constraints】
     * - isUpdateSupport=true 时，已存在门店会更新
     * - isUpdateSupport=false 时，已存在门店会报错
     *
     * 【Pitfalls】
     * - 【教训2024-03】导入时不校验子表ID，导致子表被覆盖而非更新
     * - forEach 内 try-catch 不会自动回滚事务
     *
     * @param file Excel文件
     * @param updateSupport 是否支持更新
     * @return 导入结果
     */
    @PostMapping("/import")
    @Operation(summary = "导入门店")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "file", description = "Excel 文件", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('business:table:import')")
    public CommonResult<StoreImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<StoreImportExcelVO> list = ExcelUtils.read(file, StoreImportExcelVO.class);
        return success(storeService.importStoreList(list, updateSupport));
    }

    /**
     * 转换行政区划代码为地名
     *
     * 【What】使用 AreaUtils 将 regionCode 转换为可读的地名
     *
     * @param respVO 响应VO
     * @param regionCode 行政区划代码
     */
    private void convertRegionName(StoreRespVO respVO, String regionCode) {
        if (StrUtil.isNotBlank(regionCode)) {
            try {
                Integer code = Integer.parseInt(regionCode);
                respVO.setRegionName(AreaUtils.format(code));
            } catch (NumberFormatException e) {
                respVO.setRegionName(regionCode);
            }
        }
    }

    /**
     * CSV字段转义
     *
     * 【What】处理CSV特殊字符，避免解析错误
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

}
