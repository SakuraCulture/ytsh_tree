package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * SKU条码新增/修改 Request VO
 *
 * 本VO用于创建和更新UPC（条码）的主数据，是前后端数据传输的核心对象。
 *
 * **业务说明：**
 * - 本VO为新增和修改共用，通过productUpcId字段区分：
 *   - productUpcId为null：表示新增操作
 *   - productUpcId有值：表示修改操作
 * - 使用jakarta.validation注解进行请求参数校验
 * - 校验失败时抛出MethodArgumentNotValidException
 *
 * **与Response VO的区别：**
 * | 特性 | UpcTableSaveReqVO | UpcTableRespVO |
 * |------|-------------------|----------------|
 * | 用途 | 请求（输入） | 响应（输出） |
 * | 校验 | 有@NotNull/@NotBlank注解 | 无校验注解 |
 * | createTime | 无（由系统生成） | 有 |
 * | updateTime | 无（由系统生成） | 有 |
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | 主键 | productUpcId | 修改时必填，新增时为null |
 * | SKU关联 | productSkuId | 所属SKU ID（必填） |
 * | 条码属性 | productUpcType, productUpcValue | 条码类型和值（必填） |
 * | 标记属性 | productUpcIsPrimary | 是否主条码 |
 * | 状态属性 | productUpcStatus | 启用/禁用状态 |
 *
 * **设计思路：**
 * - 新增/修改共用VO的原因：
 *   1. 减少VO类数量，降低代码复杂度
 *   2. 新增和修改的字段基本一致，共用更高效
 *   3. 通过主键是否为空判断操作类型
 * - 必填字段精简的原因：
 *   1. SKU关联和条码属性是条码的核心信息
 *   2. 主条码标记和状态有默认值，可以不传
 *
 * **使用场景：**
 * 1. 创建新条码：productUpcId为null，必填字段校验
 * 2. 更新条码：productUpcId有值，可部分更新字段
 * 3. 设置主条码：设置productUpcIsPrimary为1
 *
 * **校验规则：**
 * - productSkuId：@NotNull，不能为空
 * - productUpcType：@NotBlank，不能为空
 * - productUpcValue：@NotBlank，不能为空
 *
 * **潜在隐患及规避建议：**
 * 1. 主条码标记竞态：并发设置主条码可能导致多个主条码
 *    规避：在Service层使用事务，先清除同SKU的其他主条码
 * 2. 条码值重复：同一SKU下可能存在重复的条码值
 *    规避：在数据库层建立唯一索引
 * 3. SKU不存在：productSkuId指向不存在的SKU
 *    规避：在Service层校验SKU是否存在
 *
 */
@Schema(description = "管理后台 - SKU条码新增/修改 Request VO")
@Data
public class UpcTableSaveReqVO {

    // ==================== 主键字段 ====================

    /**
     * UPC ID
     *
     * 【业务含义】条码记录的主键
     * 【操作类型判断】
     * - null：表示新增操作
     * - 有值：表示修改操作
     * 【必填性】
     * - 新增时：可为空（系统自动生成）
     * - 修改时：必填（用于定位要更新的记录）
     */
    @Schema(description = "UPC ID", example = "1")
    private Long productUpcId;

    // ==================== SKU关联字段 ====================

    /**
     * 所属SKU ID
     *
     * 【业务含义】该条码所属的SKU主键ID
     * 【数据类型】Long
     * 【校验规则】@NotNull，不能为空
     * 【错误信息】"所属SKU ID不能为空"
     * 【关联说明】外键关联product_sku_table表
     * 【注意事项】
     * - 新增时必填
     * - 修改时如传入则校验SKU是否存在
     */
    @Schema(description = "所属SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属SKU ID不能为空")
    private Long productSkuId;

    // ==================== 条码属性字段 ====================

    /**
     * 条码类型
     *
     * 【业务含义】通用产品码的类型
     * 【数据类型】String
     * 【校验规则】@NotBlank，不能为空
     * 【错误信息】"条码类型不能为空"
     * 【取值范围】
     * - "EAN-13"：国际商品码，13位数字
     * - "UPC-A"：美国产品码，12位数字
     * - "CODE128"：物流用条码，可变长度
     * 【使用场景】
     * - 扫码解析时的格式识别
     * - 条码类型筛选
     */
    @Schema(description = "条码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "EAN-13")
    @NotBlank(message = "条码类型不能为空")
    private String productUpcType;

    /**
     * 条码值
     *
     * 【业务含义】商品条码的具体数值
     * 【数据类型】String
     * 【校验规则】@NotBlank，不能为空
     * 【错误信息】"条码值不能为空"
     * 【数据格式】
     * - EAN-13：13位数字
     * - UPC-A：12位数字
     * - CODE128：可变长度
     * 【唯一性】同一SKU下应唯一
     * 【敏感信息】是商品的重要标识信息
     */
    @Schema(description = "条码值", requiredMode = Schema.RequiredMode.REQUIRED, example = "6901234567890")
    @NotBlank(message = "条码值不能为空")
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
     * 【默认值】如不传入，默认为0（非主条码）
     * 【业务规则】
     * - 同一SKU下只能有一个主条码
     * - 设置为主条码时会自动清除同SKU的其他主条码标记
     * 【使用场景】
     * - 指定默认扫码条码
     * - 变更默认条码
     */
    @Schema(description = "是否主条码(0否1是)", example = "0")
    private Integer productUpcIsPrimary;

    // ==================== 状态属性字段 ====================

    /**
     * 条码状态
     *
     * 【业务含义】条码的启用/禁用状态
     * 【数据类型】Integer
     * 【取值范围】
     * - 0：禁用
     * - 1：启用
     * 【默认值】如不传入，默认为1（启用）
     * 【业务规则】
     * - 禁用状态下条码不可用于扫码
     * - 禁用状态保留条码数据
     * 【使用场景】
     * - 启用新条码
     * - 禁用旧条码
     * - 临时禁用条码（如包装更换过渡期）
     */
    @Schema(description = "状态(0禁用1启用)", example = "1")
    private Integer productUpcStatus;

}
