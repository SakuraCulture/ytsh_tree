package cn.iocoder.yudao.module.business.controller.admin.product;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.module.business.service.product.UpcTableService;

/**
 * SKU条码管理控制器
 *
 * 本控制器负责管理后台系统中SKU条码（UPC）的RESTful API接口。
 *
 * **业务说明：**
 * - UPC（Universal Product Code）是商品条码的一种，用于唯一标识商品
 * - 一个SKU（库存量单位）可以对应多个UPC码，例如不同包装规格、不同条形码类型
 * - 支持EAN-13、UPC-A、CODE128等多种条码类型
 * - 每个SKU可设置一个主条码（productUpcIsPrimary=1），用于默认扫描识别
 *
 * **模块职责：**
 * 1. SKU条码的增删改查操作
 * 2. 根据SKU ID查询关联的所有条码列表
 * 3. 条码值的唯一性校验，避免重复录入
 * 4. 主条码标记管理，确保同一SKU下仅有一个主条码
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
 * - 主条码互斥设计：同一SKU下只能有一个主条码
 *   原因：零售场景中扫描默认使用主条码，多个主条码会导致歧义
 *
 * **与其他模块的关联：**
 * - 依赖SkuTableModule：每个UPC必须关联一个有效的SKU（productSkuId）
 * - 被SpuTableController的导入功能调用：支持SPU/SKU/UPC联合导入
 *
 * **潜在隐患及规避建议：**
 * 1. 条码值重复风险：虽有空值校验，但建议在数据库层设置唯一索引
 * 2. 主条码标记竞态：当并发创建/更新同一SKU的主条码时，可能出现多个主条码
 *    规避：在Service层使用事务保证原子性，当前实现为串行处理
 * 3. 大数据量查询：getUpcTableListByProductSkuId无分页，SKU下UPC过多时可能性能问题
 *    建议：实际业务中SKU下UPC数量有限（一般不超过10个），暂不需要分页
 *
 */
@Tag(name = "管理后台 - SKU条码管理")
@RestController
@RequestMapping("/business/upc-table")
@Validated
public class UpcTableController {

    @Resource
    private UpcTableService upcTableService;

    /**
     * 创建新的SKU条码
     *
     * 【业务逻辑】
     * 1. 校验条码值是否已存在（全局唯一性）
     * 2. 若设置为主条码，则先清除同SKU下其他主条码标记
     * 3. 若未设置状态，默认设为启用（status=1）
     * 4. 插入新条码记录并返回ID
     *
     * @param createReqVO 条码创建请求对象，包含所属SKU ID、条码类型、条码值、主条码标记、状态
     * @return 新创建的条码记录ID，用于后续关联操作
     * @throws IllegalArgumentException 当必填字段缺失或格式不正确时（由@Valid触发）
     * @throws IllegalStateException 当条码值已存在时
     *
     * 【设计原因】
     * - 返回新创建的ID而非Boolean，因为创建操作后前端通常需要对新记录进行后续操作
     *   （如跳转到详情页、关联其他数据），返回ID可以减少前端再次查询的开销
     * - 默认启用状态，因为新建条码通常是立即需要使用的场景
     */
    @PostMapping("/create")
    @Operation(summary = "创建SKU条码")
    @PreAuthorize("@ss.hasPermission('business:upc-table:create')")
    public CommonResult<Long> createUpcTable(@Valid @RequestBody UpcTableSaveReqVO createReqVO) {
        return success(upcTableService.createUpcTable(createReqVO));
    }

    /**
     * 更新已有的SKU条码信息
     *
     * 【业务逻辑】
     * 1. 校验条码记录是否存在
     * 2. 校验新条码值是否与其他记录冲突（排除自身）
     * 3. 若设置为主条码，则先清除同SKU下其他主条码标记
     * 4. 更新条码记录
     *
     * 【注意事项】
     * - 同一个SKU下只能有一个主条码（productUpcIsPrimary=1）
     * - 更新条码值时需要检查唯一性，避免与其他SKU的条码冲突
     * - 该接口为完整更新，字段为null时会将数据库对应字段更新为null
     *
     * @param updateReqVO 条码更新请求对象，包含条码ID及需要更新的字段
     * @return 更新操作是否成功
     * @throws IllegalArgumentException 当条码ID不存在或必填字段验证失败时
     * @throws IllegalStateException 当条码值已存在时
     *
     * 【设计原因】
     * - 使用完整更新而非部分更新，因为更新场景通常需要确认所有字段的值
     * - 返回Boolean而非更新后的数据，因为更新操作本身不改变客户端需要显示的内容
     */
    @PutMapping("/update")
    @Operation(summary = "更新SKU条码")
    @PreAuthorize("@ss.hasPermission('business:upc-table:update')")
    public CommonResult<Boolean> updateUpcTable(@Valid @RequestBody UpcTableSaveReqVO updateReqVO) {
        upcTableService.updateUpcTable(updateReqVO);
        return success(true);
    }

    /**
     * 根据ID删除单个SKU条码
     *
     * 【业务逻辑】
     * 1. 校验条码记录是否存在
     * 2. 删除条码记录
     *
     * 【注意事项】
     * - 删除条码前请确保无业务关联（如订单、库存等），否则可能导致数据不一致
     * - 删除操作不可逆，建议前端增加二次确认
     *
     * @param productUpcId 条码记录的业务编号，用于定位要删除的记录
     * @return 删除操作是否成功
     * @throws IllegalArgumentException 当条码ID不存在时
     *
     * 【潜在风险】
     * - 未检查业务关联：直接删除可能影响其他模块数据完整性
     *   建议：在Service层增加业务关联校验或使用逻辑删除
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除SKU条码")
    @Parameter(name = "productUpcId", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:upc-table:delete')")
    public CommonResult<Boolean> deleteUpcTable(@RequestParam("productUpcId") Long productUpcId) {
        upcTableService.deleteUpcTable(productUpcId);
        return success(true);
    }

    /**
     * 根据ID获取SKU条码详情
     *
     * 【注意事项】
     * - 如果条码不存在则返回null而非抛异常，因为查询单个资源不存在是正常业务场景
     * - 返回数据不包含关联的SKU详细信息，仅返回条码自身数据
     *   如需SKU信息，请调用独立的SKU查询接口
     *
     * @param productUpcId 条码记录的业务编号
     * @return 条码详细信息，如果不存在则返回null
     * @throws IllegalArgumentException 当条码ID格式不正确时
     *
     * 【设计原因】
     * - 单个查询返回null是RESTful规范中的常见做法，表示资源不存在而非错误
     * - 不返回SKU信息是为了接口职责单一，减少不必要的数据加载
     */
    @GetMapping("/get")
    @Operation(summary = "获得SKU条码")
    @Parameter(name = "productUpcId", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:upc-table:query')")
    public CommonResult<UpcTableRespVO> getUpcTable(@RequestParam("productUpcId") Long productUpcId) {
        UpcTableDO upcTable = upcTableService.getUpcTable(productUpcId);
        return success(BeanUtils.toBean(upcTable, UpcTableRespVO.class));
    }

    /**
     * 分页查询SKU条码列表
     *
     * 支持按SKU ID、条码类型、状态等条件进行筛选，返回分页结果以应对大数据量场景。
     * 分页参数通过UpcTablePageReqVO传递，包含页码、每页大小及排序规则。
     *
     * 【使用场景】
     * - 后台管理系统的条码管理列表页
     * - 运营人员查询和筛选条码数据
     *
     * @param pageReqVO 分页查询请求对象，包含分页参数和筛选条件
     *                  - productSkuId: 所属SKU ID（可选，用于筛选特定SKU的条码）
     *                  - productUpcType: 条码类型（可选，如EAN-13、UPC-A）
     *                  - productUpcStatus: 状态（可选，0禁用1启用）
     * @return 分页后的条码列表，每页数据量由pageReqVO.pageSize控制
     * @throws IllegalArgumentException 当分页参数不合法时
     *
     * 【设计原因】
     * - 使用分页而非返回全部数据，因为条码数据量可能很大
     * - 默认排序规则由PageParam指定，通常按创建时间倒序
     */
    @GetMapping("/page")
    @Operation(summary = "获得SKU条码分页")
    @PreAuthorize("@ss.hasPermission('business:upc-table:query')")
    public CommonResult<PageResult<UpcTableRespVO>> getUpcTablePage(@Valid UpcTablePageReqVO pageReqVO) {
        PageResult<UpcTableDO> pageResult = upcTableService.getUpcTablePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, UpcTableRespVO.class));
    }

    /**
     * 根据SKU ID查询关联的所有条码列表
     *
     * 【业务说明】
     * - 一个SKU可以对应多个UPC码（不同包装、不同条形码类型等）
     * - 返回该SKU下的所有条码，包括主条码和副条码
     * - 条码列表按是否为主条码降序排列（主条码在前）
     *
     * 【使用场景】
     * - 商品详情页展示该SKU的所有可用条码
     * - 收银系统根据SKU查询所有可用条码用于扫码
     * - 库存管理系统查询某SKU的条码信息
     *
     * 【注意事项】
     * - 该接口暂未实现分页，因为单个SKU下的条码数量通常有限（一般不超过10个）
     * - 如果未来业务扩展导致条码数量大增，建议增加分页支持
     * - 返回空列表表示该SKU下没有任何条码，而非错误
     *
     * @param productSkuId 所属SKU的业务编号，用于筛选关联的条码数据
     * @return 该SKU下的所有条码列表，若无关联则返回空列表
     * @throws IllegalArgumentException 当SKU ID格式不正确时
     *
     * 【设计原因】
     * - 使用List返回而非分页，因为单个SKU下的条码数量通常有限
     *   不需要分页带来的额外开销，且前端通常需要展示全部条码供选择
     * - 返回空列表而非null，是为了让调用方无需进行空值判断
     */
    @GetMapping("/list-by-product-sku-id")
    @Operation(summary = "获得SKU条码列表")
    @Parameter(name = "productSkuId", description = "所属SKU ID")
    @PreAuthorize("@ss.hasPermission('business:upc-table:query')")
    public CommonResult<List<UpcTableRespVO>> getUpcTableListByProductSkuId(@RequestParam("productSkuId") Long productSkuId) {
        List<UpcTableDO> list = upcTableService.getUpcTableListByProductSkuId(productSkuId);
        return success(BeanUtils.toBean(list, UpcTableRespVO.class));
    }

}
