package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * SKU条码分页查询 Request VO
 *
 * 本VO用于分页查询UPC（通用产品码/条码）列表，支持按SKU和条码属性筛选。
 *
 * **业务说明：**
 * - UPC（Universal Product Code）是商品条码，用于唯一标识商品
 * - 本VO继承PageParam，获得分页能力（pageNo、pageSize）
 * - 支持按所属SKU、条码类型、状态等条件筛选
 *
 * **筛选维度：**
 * 1. SKU关联：productSkuId（筛选特定SKU下的所有条码）
 * 2. 条码属性：productUpcType（按条码类型筛选）
 * 3. 状态属性：productUpcStatus（按启用/禁用状态筛选）
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | SKU关联 | productSkuId | 所属SKU ID |
 * | 条码属性 | productUpcType | 条码类型（EAN-13/UPC-A/CODE128） |
 * | 状态属性 | productUpcStatus | 启用/禁用状态 |
 *
 * **设计思路：**
 * - 继承PageParam的原因：
 *   1. 复用分页参数，避免重复定义pageNo、pageSize字段
 *   2. 符合面向对象设计原则，减少样板代码
 * - 筛选字段精简的原因：
 *   1. 条码数据量相对较小，不需要过多筛选条件
 *   2. 主要按SKU维度查询（一个SKU通常只有几个条码）
 *   3. 避免过度设计，保持接口简洁
 *
 * **使用场景：**
 * 1. 后台条码管理列表页：运营人员筛选和查找条码
 * 2. 条码启用/禁用批量操作
 * 3. 特定SKU的条码查询
 *
 * **潜在隐患及规避建议：**
 * 1. SKU不存在查询：当productSkuId指向不存在的SKU时，返回空分页结果
 *    说明：这是正常行为，无需特殊处理
 * 2. 条码类型模糊匹配：如需精确匹配，建议在Service层处理
 *
 */
@Schema(description = "管理后台 - SKU条码分页 Request VO")
@Data
public class UpcTablePageReqVO extends PageParam {

    // ==================== SKU关联字段 ====================

    /**
     * 所属SKU ID
     *
     * 【业务含义】该条码所属的SKU主键ID
     * 【数据类型】Long
     * 【筛选方式】精确匹配
     * 【使用场景】
     * - 查询某SKU下的所有条码
     * - 商品详情页展示该SKU的条码列表
     * - 收银系统查询可用的条码
     * 【关联说明】
     * - 外键关联product_sku_table表
     * - 一个SKU可以对应多个UPC条码
     * 【注意事项】
     * - 为空时表示查询所有SKU的条码
     * - 当SKU不存在时返回空分页结果
     */
    @Schema(description = "所属SKU ID", example = "1")
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
     * 【筛选方式】精确匹配或模糊匹配
     * 【使用场景】
     * - 筛选特定类型的条码（如只查询EAN-13码）
     * - 条码类型统计
     * 【注意事项】
     * - 为空时表示查询所有类型
     * - 建议在枚举类中定义支持的类型
     */
    @Schema(description = "条码类型", example = "EAN-13")
    private String productUpcType;

    // ==================== 状态属性字段 ====================

    /**
     * 条码状态
     *
     * 【业务含义】条码的启用/禁用状态
     * 【数据类型】Integer
     * 【取值范围】
     * - 0：禁用（不可用于扫码）
     * - 1：启用（可用于扫码）
     * 【筛选方式】精确匹配
     * 【业务规则】
     * - 禁用状态下条码不可用于收银扫码
     * - 禁用状态保留条码数据，用于历史追溯
     * - 常用于包装更换过渡期的条码切换
     * 【使用场景】
     * - 筛选已启用的条码用于销售
     * - 查看禁用的条码用于问题排查
     * - 批量启用/禁用操作
     * 【默认值】为空时表示查询所有状态
     */
    @Schema(description = "状态(0禁用1启用)", example = "1")
    private Integer productUpcStatus;

}
