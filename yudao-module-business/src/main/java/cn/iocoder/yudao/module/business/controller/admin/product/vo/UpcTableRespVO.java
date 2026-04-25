package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import lombok.*;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SKU条码 Response VO
 *
 * 本VO用于返回UPC（通用产品码/条码）的完整信息，是前端展示条码详情的数据载体。
 *
 * **业务说明：**
 * - UPC（Universal Product Code）是商品条码的一种，用于唯一标识商品
 * - 一个SKU可以对应多个UPC码（如不同包装规格、不同条形码类型）
 * - 每个SKU可设置一个主条码，用于默认扫描识别
 *
 * **与SKU的关系：**
 * - 条码必须归属于某个SKU
 * - 一个SKU可以有多个条码，但只能有一个主条码
 * - 关系：多对一（N个UPC -> 1个SKU）
 *
 * **使用场景：**
 * 1. 条码详情页：展示条码的完整信息
 * 2. 条码管理列表：展示条码列表供运营人员管理
 * 3. 商品详情页：展示该SKU的所有条码
 * 4. 收银系统：扫码识别商品
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | 基本信息 | productUpcId | 条码主键 |
 * | SKU关联 | productSkuId | 所属SKU ID |
 * | 条码属性 | productUpcType, productUpcValue | 条码类型和值 |
 * | 标记属性 | productUpcIsPrimary | 是否主条码 |
 * | 状态属性 | productUpcStatus | 启用/禁用状态 |
 * | 时间属性 | createTime, updateTime | 创建和更新时间 |
 *
 * **设计思路：**
 * - Response VO与DO分离的原因：
 *   1. API稳定性：数据库字段变更不影响前端接口契约
 *   2. 数据过滤：隐藏内部字段，只返回必要的业务数据
 *   3. 格式化：对时间等字段进行格式化处理
 * - 使用@Builder模式的原因：
 *   1. 便于测试用例构建对象
 *   2. 代码可读性更好，字段赋值清晰
 *
 * **潜在隐患及规避建议：**
 * 1. 条码值显示风险：productUpcValue是敏感信息，展示会暴露商品身份
 *    建议：在权限控制上限制查看范围
 * 2. 主条码标记不准确：当并发更新时可能出现多个主条码
 *    规避：在Service层使用事务保证原子性
 *
 */
@Schema(description = "管理后台 - SKU条码 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcTableRespVO {

    // ==================== 基本信息字段 ====================

    /**
     * UPC ID
     *
     * 【业务含义】条码记录的主键，唯一标识一个条码
     * 【数据类型】Long，长整型
     * 【取值说明】由数据库自增生成，全球唯一
     * 【使用场景】
     * - 条码查询和更新的操作依据
     * - 日志记录和数据统计的主键
     */
    @Schema(description = "UPC ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long productUpcId;

    // ==================== SKU关联字段 ====================

    /**
     * 所属SKU ID
     *
     * 【业务含义】该条码所属的SKU主键ID
     * 【数据类型】Long
     * 【关联说明】外键关联product_sku_table表
     * 【使用场景】
     * - 查询条码所属的商品信息
     * - 数据关联和追溯
     */
    @Schema(description = "所属SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long productSkuId;

    // ==================== 条码属性字段 ====================

    /**
     * 条码类型
     *
     * 【业务含义】通用产品码的类型，决定了编码规则和长度
     * 【数据类型】String
     * 【取值范围】
     * - "EAN-13"：国际商品码，13位数字，我国使用此标准
     * - "UPC-A"：美国产品码，12位数字
     * - "CODE128"：物流用条码，支持字母数字，可变长度
     * 【使用场景】
     * - 条码类型展示
     * - 扫码识别时的解析规则依据
     */
    @Schema(description = "条码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "EAN-13")
    private String productUpcType;

    /**
     * 条码值
     *
     * 【业务含义】商品条码的具体数值，是商品在流通环节的唯一身份标识
     * 【数据类型】String
     * 【数据格式】
     * - EAN-13：13位数字，如"6901234567890"
     * - UPC-A：12位数字，如"012345678905"
     * - CODE128：可变长度，支持数字、字母、特殊字符
     * 【使用场景】
     * - 收银系统扫码识别
     * - 库存管理扫描
     * - 物流追溯
     * 【敏感信息说明】
     * - 条码值是商品的重要标识信息
     * - 建议对普通用户隐藏完整条码值
     */
    @Schema(description = "条码值", requiredMode = Schema.RequiredMode.REQUIRED, example = "6901234567890")
    private String productUpcValue;

    // ==================== 标记属性字段 ====================

    /**
     * 是否主条码
     *
     * 【业务含义】标识该UPC码是否为默认主码
     * 【数据类型】Integer
     * 【取值范围】
     * - 0：否（副条码）
     * - 1：是（主条码）
     * 【业务规则】
     * - 同一SKU下只能有一个主条码（值为1）
     * - 主条码用于默认扫码识别
     * - 收银系统优先匹配主条码
     * 【使用场景】
     * - 标记默认扫描条码
     * - 库存关联默认使用主条码
     */
    @Schema(description = "是否主条码(0否1是)", example = "1")
    private Integer productUpcIsPrimary;

    // ==================== 状态属性字段 ====================

    /**
     * 条码状态
     *
     * 【业务含义】条码的启用/禁用状态
     * 【数据类型】Integer
     * 【取值范围】
     * - 0：禁用（不可用于扫码）
     * - 1：启用（可用于扫码）
     * 【业务规则】
     * - 禁用状态下条码不可用于收银扫码
     * - 禁用状态保留条码数据，用于历史追溯
     * - 常用于包装更换过渡期的条码切换
     * 【状态转换】
     * - 启用 -> 禁用：手动禁用、关联SKU下架
     * - 禁用 -> 启用：手动启用
     */
    @Schema(description = "状态(0禁用1启用)", example = "1")
    private Integer productUpcStatus;

    // ==================== 时间属性字段 ====================

    /**
     * 创建时间
     *
     * 【业务含义】条码记录的创建时间
     * 【数据类型】LocalDateTime
     * 【使用场景】
     * - 按创建时间排序
     * - 数据统计
     * - 操作日志追溯
     * 【日期格式】yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     *
     * 【业务含义】条码记录的最后更新时间
     * 【数据类型】LocalDateTime
     * 【更新时机】
     * - 条码信息被修改时自动更新
     * - 主条码标记变更时更新
     * - 状态变更时更新
     * 【使用场景】
     * - 数据同步判断依据
     * - 缓存失效判断
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
