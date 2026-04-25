package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SPU/SKU/UPC联合导入数据对象
 *
 * 本VO用于Excel批量导入商品主数据，支持一次性导入SPU（商品基础信息）、
 * SKU（销售单元）和UPC（条码）三层关联数据。
 *
 * **数据结构说明：**
 * - 采用扁平化设计，将SPU、SKU、UPC三层数据合并为一行
 * - 同一SPU编码的多行数据表示该SPU下的多个SKU
 * - 同一SKU编码的多行数据表示该SKU下的多个UPC条码
 *
 * **数据层级关系：**
 * ```
 * SPU（商品基础信息）
 *   └── SKU（销售单元，可多个）
 *         └── UPC（条码，可多个）
 * ```
 *
 * **导入流程说明：**
 * 1. 解析Excel每一行数据
 * 2. 根据SPU编码判断是否已存在：
 *    - 不存在则新建SPU记录
 *    - 存在则根据updateSupport参数决定是否更新
 * 3. 根据SKU编码判断是否已存在（同一SPU下）：
 *    - 不存在则新建SKU记录
 *    - 存在则根据updateSupport参数决定是否更新
 * 4. 根据UPC码值判断是否已存在（同一SKU下）：
 *    - 不存在则新建UPC记录
 *    - 存在则跳过（不重复创建）
 *
 * **字段分组及说明：**
 * | 分组 | 字段前缀 | 说明 |
 * |------|----------|------|
 * | SPU基础信息 | productSpu* | 商品名称、品牌、产地、厂商等 |
 * | SKU销售单元 | productSku* | 商品编码、名称、规格尺寸、价格等 |
 * | UPC条码信息 | productUpc* | 条码类型、条码值、主码标记、状态等 |
 *
 * **导入校验规则：**
 * 1. SPU编码为必填项，不可为空
 * 2. SPU编码全局唯一，不可重复
 * 3. SKU编码在同一SPU下唯一
 * 4. UPC码值在同一SKU下唯一
 * 5. EAN码为13位数字，UPC-A为12位数字
 *
 * **设计思路：**
 * - 采用扁平化而非嵌套结构的原因：
 *   1. Excel表格天然是扁平结构，便于用户理解和填写
 *   2. 减少数据冗余：SPU字段只需填写一次
 *   3. 符合用户操作习惯：批量导入时按行填写更直观
 * - 使用@ExcelProperty指定列名，与导入模板表头对应
 * - 使用@ExcelIgnoreUnannotated忽略未标注的字段，避免导出时暴露多余字段
 *
 * **与ServiceImpl的对应关系：**
 * - 导入逻辑主要在SpuTableServiceImpl.importSpuSkuUpcList()实现
 * - 使用HashMap缓存已创建的SPU和SKU，减少数据库查询
 * - 关联关系通过SPU编码和SKU编码维护，而非ID
 *
 * **潜在隐患及规避建议：**
 * 1. 大数据量导入性能：循环内多次数据库操作，数据量大时可能较慢
 *    规避：分批导入或使用异步任务处理
 * 2. 内存占用：使用HashMap缓存，数据量大时内存占用增加
 *    建议：控制单次导入数据量，或使用数据库临时表
 * 3. 数据一致性：导入过程中异常可能导致部分数据已提交
 *    当前：使用事务保证原子性，但无法回滚已成功的行
 * 4. SKU与SPU的分离：只根据SKU编码判断是否已存在，不校验SPU归属
 *    风险：不同SPU下相同编码的SKU可能被错误关联
 *    规避：确保SKU编码全局唯一
 *
 * **使用示例：**
 * | SPU编码 | SPU名称 | SKU编码 | UPC码类型 | UPC码值 | 是否主码 |
 * |---------|---------|---------|-----------|---------|----------|
 * | SPU001 | 示例商品 | SKU001-白 | EAN-13 | 1234567890123 | 1 |
 * | SPU001 | 示例商品 | SKU001-白 | UPC-A | 012345678905 | 0 |
 * | SPU001 | 示例商品 | SKU001-黑 | EAN-13 | 1234567890124 | 1 |
 * | SPU002 | 示例商品2 | SKU002-001 | EAN-13 | 9876543210987 | 1 |
 * 
 */
@Schema(description = "管理后台 - SPU/SKU/UPC导入数据 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class SpuSkuUpcImportVO {

    // ==================== SPU基础信息字段 ====================

    /**
     * SPU编码
     *
     * 【业务含义】商品的标准产品单元编码，用于全局唯一标识一个商品
     * 【必填性】必填项，导入时不能为空
     * 【校验规则】建议全局唯一，不可重复
     * 【关联说明】同一SPU编码的多行数据会被聚合到同一个SPU下
     */
    @ExcelProperty("SPU编码")
    @Schema(description = "SPU编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productSpuCode;

    /**
     * SPU名称
     *
     * 【业务含义】商品的标准产品单元名称，即商品通用名称
     * 【示例】"iPhone 15 Pro"、"可口可乐330ml"
     * 【关联说明】同一SPU编码的多行数据通常使用相同的SPU名称
     */
    @ExcelProperty("SPU名称")
    @Schema(description = "SPU名称")
    private String productSpuName;

    /**
     * 品牌
     *
     * 【业务含义】商品所属品牌，用于品牌筛选和展示
     * 【示例】"Apple"、" Coca-Cola"
     */
    @ExcelProperty("品牌")
    @Schema(description = "品牌")
    private String productBrand;

    /**
     * 分类ID
     *
     * 【业务含义】商品所属分类的编号，关联分类表
     * 【数据类型】Long类型，对应分类主键
     * 【注意事项】需确保分类ID在系统中已存在
     */
    @ExcelProperty("分类ID")
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 产地
     *
     * 【业务含义】商品的生产地或原产地
     * 【示例】"中国"、"美国"、"日本"
     */
    @ExcelProperty("产地")
    @Schema(description = "产地")
    private String productOrigin;

    /**
     * 生产商
     *
     * 【业务含义】商品的生产制造商名称
     * 【示例】"苹果电子产品公司"、"可口可乐公司"
     */
    @ExcelProperty("生产商")
    @Schema(description = "生产商")
    private String productManufacturer;

    /**
     * 规格模板
     *
     * 【业务含义】商品规格的参数模板，用于标准化商品规格描述
     * 【示例】"标准规格"、"手机规格模板"
     */
    @ExcelProperty("规格模板")
    @Schema(description = "规格模板")
    private String productSpecTemplate;

    /**
     * SPU状态
     *
     * 【业务含义】商品的上架/下架状态
     * 【取值范围】0-下架，1-上架
     * 【默认行为】如为空，默认设为上架状态
     */
    @ExcelProperty("SPU状态")
    @Schema(description = "SPU状态(0下架1上架)")
    private Integer productSpuStatus;

    // ==================== SKU销售单元字段 ====================

    /**
     * SKU编码
     *
     * 【业务含义】库存量单位的编码，用于唯一标识具体销售单元
     * 【必填性】可选，但建议填写以区分不同款式/规格
     * 【校验规则】同一SPU下编码唯一
     * 【关联说明】同一SKU编码的多行数据会被聚合到同一个SKU下
     */
    @ExcelProperty("SKU编码")
    @Schema(description = "SKU编码")
    private String productSkuCode;

    /**
     * SKU名称
     *
     * 【业务含义】库存量单位的名称，通常包含款式/规格信息
     * 【示例】"白色款"、"XL码"、"便携装"
     */
    @ExcelProperty("SKU名称")
    @Schema(description = "SKU名称")
    private String productSkuName;

    /**
     * EAN码
     *
     * 【业务含义】European Article Number，欧洲商品编码，13位数字
     * 【数据格式】13位数字，如6901234567890
     * 【校验建议】应符合EAN-13校验位规则
     * 【使用场景】国际商品流通、零售结算
     */
    @ExcelProperty("EAN码")
    @Schema(description = "EAN码(13位)")
    private String productSkuEan;

    /**
     * 重量
     *
     * 【业务含义】商品的重量，用于物流计费和重量统计
     * 【数据类型】BigDecimal，保留精度
     * 【单位】需配合productWeightUnit字段使用
     */
    @ExcelProperty("重量")
    @Schema(description = "重量")
    private BigDecimal productWeight;

    /**
     * 重量单位
     *
     * 【业务含义】重量的计量单位
     * 【取值示例】"g"（克）、"kg"（千克）、"lb"（磅）
     */
    @ExcelProperty("重量单位")
    @Schema(description = "重量单位")
    private String productWeightUnit;

    /**
     * 长度
     *
     * 【业务含义】商品的外包装长度
     * 【单位】厘米（cm）
     * 【使用场景】物流体积计算、仓储布局
     */
    @ExcelProperty("长度(cm)")
    @Schema(description = "长度(cm)")
    private BigDecimal productLength;

    /**
     * 宽度
     *
     * 【业务含义】商品的外包装宽度
     * 【单位】厘米（cm）
     * 【使用场景】物流体积计算、仓储布局
     */
    @ExcelProperty("宽度(cm)")
    @Schema(description = "宽度(cm)")
    private BigDecimal productWidth;

    /**
     * 高度
     *
     * 【业务含义】商品的外包装高度
     * 【单位】厘米（cm）
     * 【使用场景】物流体积计算、仓储布局
     */
    @ExcelProperty("高度(cm)")
    @Schema(description = "高度(cm)")
    private BigDecimal productHeight;

    /**
     * 成本价
     *
     * 【业务含义】商品的基准采购成本，用于成本核算和利润分析
     * 【数据类型】BigDecimal，保留精度
     * 【注意事项】此为含税成本价，不同供应商可能不同
     */
    @ExcelProperty("成本价")
    @Schema(description = "基准成本价")
    private BigDecimal productCostPrice;

    /**
     * 零售价
     *
     * 【业务含义】商品的基准零售价格，用于销售定价指导
     * 【数据类型】BigDecimal，保留精度
     * 【注意事项】此为建议零售价，实际售价可能因活动调整
     */
    @ExcelProperty("零售价")
    @Schema(description = "基准零售价")
    private BigDecimal productRetailPrice;

    /**
     * SKU状态
     *
     * 【业务含义】销售单元的上架/下架状态
     * 【取值范围】0-下架，1-上架
     * 【默认行为】如为空，默认设为上架状态
     */
    @ExcelProperty("SKU状态")
    @Schema(description = "SKU状态(0下架1上架)")
    private Integer productSkuStatus;

    // ==================== UPC条码字段 ====================

    /**
     * UPC码类型
     *
     * 【业务含义】通用产品码的类型，决定了编码规则和长度
     * 【取值范围】
     * - "EAN-13"：国际商品码，13位数字，我国使用此标准
     * - "UPC-A"：美国产品码，12位数字
     * - "CODE128"：物流用条码，支持字母数字，可变长度
     * 【校验建议】根据类型验证码值格式是否符合对应标准
     */
    @ExcelProperty("UPC码类型")
    @Schema(description = "UPC码类型(UPC-A/EAN-13/CODE128)")
    private String productUpcType;

    /**
     * UPC码值
     *
     * 【业务含义】商品条码的具体数值，是商品在流通环节的唯一身份标识
     * 【必填性】可选，但建议填写以支持扫码功能
     * 【校验规则】
     * - EAN-13：13位数字，最后一位为校验位
     * - UPC-A：12位数字，最后一位为校验位
     * - CODE128：可变长度，支持数字、字母、特殊字符
     * 【关联说明】同一SKU下可以有多个UPC码（如不同包装规格）
     */
    @ExcelProperty("UPC码值")
    @Schema(description = "UPC码值")
    private String productUpcValue;

    /**
     * 是否主码
     *
     * 【业务含义】标识该UPC码是否为默认主码，用于扫码识别和库存关联
     * 【取值范围】0-否，1-是
     * 【业务规则】同一SKU下只能有一个主码（值为1）
     * 【默认行为】如为空，默认为非主码
     * 【使用场景】
     * - 收银系统扫码默认使用主码
     * - 库存关联默认使用主码
     * - 商品搜索优先匹配主码
     */
    @ExcelProperty("是否主码")
    @Schema(description = "是否主码(0否1是)")
    private Integer productUpcIsPrimary;

    /**
     * UPC状态
     *
     * 【业务含义】条码的启用/禁用状态
     * 【取值范围】0-禁用，1-启用
     * 【业务说明】
     * - 禁用状态下条码不可用于扫码，但数据保留
     * - 用于临时禁用某条码（如包装更换过渡期）
     * 【默认行为】如为空，默认设为启用状态
     */
    @ExcelProperty("UPC状态")
    @Schema(description = "UPC状态(0禁用1启用)")
    private Integer productUpcStatus;
}
