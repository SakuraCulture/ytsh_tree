package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;

/**
 * SPU基础分类新增/修改 Request VO
 *
 * 本VO用于创建和更新SPU（标准产品单元）的主数据，是前后端数据传输的核心对象。
 *
 * **业务说明：**
 * - 本VO为新增和修改共用，通过productSpuId字段区分：
 *   - productSpuId为null：表示新增操作
 *   - productSpuId有值：表示修改操作
 * - 支持同时维护SPU及其子表SKU的数据
 * - 使用jakarta.validation注解进行请求参数校验
 *
 * **与Response VO的区别：**
 * | 特性 | SpuTableSaveReqVO | SpuTableRespVO |
 * |------|-------------------|----------------|
 * | 用途 | 请求（输入） | 响应（输出） |
 * | SKU类型 | SkuTableDO（DO对象） | SkuTableRespVO（VO对象） |
 * | 校验 | 有@Valid注解 | 无校验注解 |
 * | createTime | 无（由系统生成） | 有 |
 * | Excel注解 | 无 | 有@ExcelProperty |
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | 主键 | productSpuId | 修改时必填，新增时为null |
 * | 基本信息 | productSpuCode, productSpuName, productBrand | SPU标识和品牌 |
 * | 分类属性 | categoryId | 商品所属分类 |
 * | 供应商属性 | productOrigin, productManufacturer, productSpecTemplate | 产地和规格 |
 * | 图片属性 | productImageUrl, productDetailImages | 商品展示图片 |
 * | 描述属性 | productDescription | 商品详细描述 |
 * | 状态属性 | productSpuStatus | 上下架状态 |
 * | SKU列表 | skuTables | 关联的SKU列表（批量维护） |
 *
 * **设计思路：**
 * - 新增/修改共用VO的原因：
 *   1. 减少VO类数量，降低代码复杂度
 *   2. 新增和修改的字段基本一致，共用更高效
 *   3. 通过主键是否为空判断操作类型
 * - skuTables使用DO而非VO的原因：
 *   1. 减少对象转换，直接入库
 *   2. Service层复用DO到DO的转换逻辑
 *   3. 输入对象不需要RespVO的格式化数据
 *
 * **使用场景：**
 * 1. 创建新SPU：productSpuId为null，skuTables为新增的SKU列表
 * 2. 更新SPU：productSpuId有值，skuTables为需要更新的SKU列表
 * 3. 更新SPU并批量维护SKU：同时修改SPU信息和SKU列表
 *
 * **校验规则：**
 * - productSpuCode：建议全局唯一
 * - productSpuName：非空，长度限制
 * - categoryId：外键关联，需确保存在
 * - productSkuEan：EAN码格式校验（13位数字）
 * - skuTables中的每个SKU也需要符合校验规则
 *
 * **潜在隐患及规避建议：**
 * 1. 批量SKU维护风险：skuTables为全量替换，非增量更新
 *    说明：传入的SKU列表会完全替换原有SKU，需确保数据完整
 * 2. 修改时SKU关联丢失：skuTables传入空列表会清空所有SKU
 *    规避：修改操作前先查询现有SKU，确保数据完整
 * 3. 循环引用风险：Service层处理skuTables时注意递归调用
 *    规避：在Service层使用事务保证数据一致性
 *
 */
@Schema(description = "管理后台 - SPU基础分类新增/修改 Request VO")
@Data
public class SpuTableSaveReqVO {

    // ==================== 主键字段 ====================

    /**
     * SPU ID
     *
     * 【业务含义】SPU记录的主键
     * 【操作类型判断】
     * - null：表示新增操作
     * - 有值：表示修改操作
     * 【必填性】
     * - 新增时：可为空（系统自动生成）
     * - 修改时：必填（用于定位要更新的记录）
     */
    @Schema(description = "SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20081")
    private Long productSpuId;

    // ==================== SPU基本信息字段 ====================

    /**
     * SPU编码
     *
     * 【业务含义】SPU的业务编码，用于唯一标识和检索
     * 【必填性】建议必填，用于快速定位SPU
     * 【校验规则】
     * - 建议全局唯一
     * - 长度限制：通常不超过50字符
     * - 格式：字母、数字、连字符的组合
     * 【修改说明】修改时需校验编码唯一性
     */
    @Schema(description = "SPU编码")
    private String productSpuCode;

    /**
     * SPU名称
     *
     * 【业务含义】商品的标准产品单元名称
     * 【必填性】建议必填
     * 【校验规则】
     * - 非空校验
     * - 长度限制：通常不超过200字符
     * 【命名规范】应描述商品的本质特征，不包含具体销售属性
     */
    @Schema(description = "SPU名称", example = "芋艿")
    private String productSpuName;

    /**
     * 品牌
     *
     * 【业务含义】商品所属品牌名称
     * 【使用场景】
     * - 品牌筛选和搜索
     * - 品牌商品统计
     * 【校验建议】长度限制，通常不超过100字符
     */
    @Schema(description = "品牌")
    private String productBrand;

    // ==================== SPU分类属性字段 ====================

    /**
     * 分类ID
     *
     * 【业务含义】商品所属分类的主键ID
     * 【数据类型】Long
     * 【校验规则】需确保对应的分类记录存在
     * 【修改说明】修改时如传入新分类，需校验分类有效性
     */
    @Schema(description = "分类ID", example = "27572")
    private Long categoryId;

    // ==================== SPU供应商属性字段 ====================

    /**
     * 产地
     *
     * 【业务含义】商品的生产地或原产地
     * 【使用场景】
     * - 进口商品标识
     * - 产地追溯
     * 【示例】"中国"、"日本"、"美国"
     */
    @Schema(description = "产地")
    private String productOrigin;

    /**
     * 生产商
     *
     * 【业务含义】商品的生产制造企业名称
     * 【使用场景】
     * - 供应商管理
     * - 质量追溯
     * 【校验建议】长度限制，通常不超过200字符
     */
    @Schema(description = "生产商")
    private String productManufacturer;

    /**
     * 规格模板
     *
     * 【业务含义】商品规格的参数模板名称
     * 【使用场景】
     * - 规格标准化管理
     * - 同模板商品归类
     * 【校验建议】需确保模板名称在系统中已定义
     */
    @Schema(description = "规格模板")
    private String productSpecTemplate;

    // ==================== SPU图片属性字段 ====================

    /**
     * 商品主图URL
     *
     * 【业务含义】SPU主图的访问地址
     * 【数据类型】String，完整的HTTP/HTTPS URL
     * 【校验规则】
     * - URL格式校验
     * - 建议使用CDN托管的图片
     * 【注意事项】
     * - 传入空字符串表示清除主图
     * - 建议先上传图片获得URL再保存
     */
    @Schema(description = "商品主图URL", example = "https://www.iocoder.cn")
    private String productImageUrl;

    /**
     * 商品详情图片
     *
     * 【业务含义】SPU详情页的图片组
     * 【数据格式】
     * - 逗号分隔的URL列表："url1,url2,url3"
     * - 或JSON数组字符串
     * 【校验建议】
     * - 建议控制图片数量（通常不超过10张）
     * - 每张图片URL需符合URL格式
     */
    @Schema(description = "商品详情图片")
    private String productDetailImages;

    // ==================== SPU描述属性字段 ====================

    /**
     * 商品描述
     *
     * 【业务含义】商品的详细描述文本
     * 【数据格式】纯文本或HTML富文本
     * 【校验规则】
     * - 长度限制：通常不超过5000字符
     * - 富文本内容需进行XSS过滤
     * 【修改说明】传入新值会完全覆盖旧值
     */
    @Schema(description = "商品描述", example = "随便")
    private String productDescription;

    // ==================== SPU状态属性字段 ====================

    /**
     * SPU状态
     *
     * 【业务含义】商品的上架/下架状态
     * 【取值范围】
     * - 0：下架
     * - 1：上架
     * 【默认值】如不传入，默认设为上架状态
     * 【业务规则】
     * - 新增时：默认上架
     * - 修改时：可切换上下架状态
     */
    @Schema(description = "状态(0下架1上架)", example = "2")
    private Integer productSpuStatus;

    // ==================== SKU列表字段（批量维护） ====================

    /**
     * SKU商品主数据列表
     *
     * 【业务含义】该SPU下的所有SKU列表，用于批量创建或更新SKU
     * 【数据类型】List<SkuTableDO>
     * 【业务规则】
     * - 新增时：传入要创建的SKU列表
     * - 修改时：传入完整的SKU列表（全量替换，非增量）
     *
     * 【重要说明：全量替换而非增量更新】
     * - 传入的skuTables会完全替换原有SKU
     * - 如需保留原有SKU，必须将原有SKU也传入列表
     * - 空列表表示清空所有SKU
     *
     * 【SKU处理逻辑】
     * 1. 先删后增：根据SKU编码对比，新增不存在的、更新已存在的
     * 2. diffList算法：对比新旧列表，找出需要新增、修改、删除的SKU
     * 3. 事务保证：所有SKU操作在同一事务中，失败全部回滚
     *
     * 【设计原因】
     * - 使用DO而非VO：减少对象转换，直接复用DO的校验逻辑
     * - 全量替换：简化业务逻辑，避免部分更新的边界问题
     *
     * 【修改时的注意事项】
     * 1. 先查询现有SKU列表，确保完整传入
     * 2. 如只需修改单个SKU，也需传入完整列表
     * 3. 删除操作不可逆，请谨慎处理
     *
     * 【示例】
     * - 新增SPU并创建SKU：传入新的skuTables
     * - 修改SPU并更新SKU：传入包含修改后SKU的完整列表
     * - 清除所有SKU：传入空列表
     */
    @Schema(description = "SKU商品主数据列表")
    private List<SkuTableDO> skuTables;

}
