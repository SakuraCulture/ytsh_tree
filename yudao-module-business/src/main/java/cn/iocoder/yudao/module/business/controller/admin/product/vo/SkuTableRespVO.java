package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * SKU商品主数据 Response VO
 *
 * 本VO用于返回SKU（库存量单位）的完整信息，是前端展示商品详情的重要数据载体。
 *
 * **业务说明：**
 * - SKU（Stock Keeping Unit）是库存管理的最小单位
 * - 一个SPU（标准产品单元）可以对应多个SKU（如不同颜色、尺寸、款式）
 * - SKU是实际销售和库存管理的实体，具有独立的库存、价格、图片等属性
 *
 * **与SPU的关系：**
 * - SPU是商品信息的聚合，如"iPhone 15 Pro"是一个SPU
 * - SKU是具体销售单元，如"iPhone 15 Pro 256G 银色"是一个SKU
 * - 关系：一对多（1个SPU -> N个SKU）
 *
 * **使用场景：**
 * 1. 商品详情页：展示某SKU的完整信息
 * 2. 购物车：显示用户选择的SKU信息
 * 3. 订单详情：记录用户购买的SKU
 * 4. 库存管理：管理各SKU的库存数量
 * 5. 后台管理：运营人员维护SKU信息
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | 基本信息 | productSkuId, productSkuCode, productSkuName | SKU的标识和名称 |
 * | 关联信息 | productSpuId | 所属SPU的ID |
 * | 条码信息 | productSkuEan | 主EAN条码，用于扫码识别 |
 * | 规格尺寸 | productWeight*, productLength/Width/Height | 物理规格，用于物流 |
 * | 价格信息 | productCostPrice, productRetailPrice | 成本和零售价格 |
 * | 图片信息 | productImageUrl | SKU主图 |
 * | 状态信息 | productSkuStatus | 上架/下架状态 |
 *
 * **设计思路：**
 * - Response VO与DO分离的原因：
 *   1. API稳定性：数据库字段变更不影响前端接口契约
 *   2. 数据过滤：隐藏内部字段，只返回必要的业务数据
 *   3. 格式化：对时间、金额等进行格式化处理
 * - 使用@Schema注解提供Swagger文档支持
 * - 使用Lombok@Data自动生成getter方法，减少样板代码
 *
 * **潜在隐患及规避建议：**
 * 1. 图片URL失效：productImageUrl为外部URL，可能过期或变更
 *    建议：使用CDN或图片服务，确保URL长期有效
 * 2. 价格精度问题：使用BigDecimal但未指定精度，可能出现浮点误差
 *    建议：在数据库和业务层统一价格精度（如保留2位小数）
 * 3. SKU与SPU关联未校验：productSpuId可能指向不存在的SPU
 *    建议：在Service层或数据库层建立外键约束
 *
 */
@Schema(description = "管理后台 - SKU商品主数据 Response VO")
@Data
public class SkuTableRespVO {

    // ==================== 基本信息 ====================

    /**
     * SKU ID
     *
     * 【业务含义】SKU记录的主键，唯一标识一个SKU
     * 【数据类型】Long，长整型
     * 【取值说明】由数据库自增生成，全球唯一
     * 【使用场景】
     * - 库存管理的关键标识
     * - 订单明细关联SKU
     * - 数据更新和删除的操作依据
     */
    @Schema(description = "SKU ID", example = "20081")
    private Long productSkuId;

    /**
     * SKU编码
     *
     * 【业务含义】SKU的业务编码，用于业务人员识别和检索
     * 【必填性】建议必填，用于快速定位SKU
     * 【编码规则】
     * - 通常包含分类、款式、规格等业务含义
     * - 建议全局唯一，避免不同SPU下出现相同编码
     * 【示例】"IPHONE15-PRO-256G-SILVER"（iPhone 15 Pro 256GB 银色）
     */
    @Schema(description = "SKU编码")
    private String productSkuCode;

    /**
     * SKU名称
     *
     * 【业务含义】SKU的商品名称，通常包含具体规格信息
     * 【命名规范】应包含可区分该SKU的关键属性
     * 【示例】"iPhone 15 Pro 256GB 银色"、"可口可乐 330ml 罐装"
     */
    @Schema(description = "SKU名称", example = "芋艿")
    private String productSkuName;

    // ==================== 关联信息 ====================

    /**
     * 所属SPU ID
     *
     * 【业务含义】该SKU所属的SPU主键，建立SKU与SPU的关联
     * 【关联关系】外键关联product_spu_table表
     * 【级联说明】
     * - 删除SPU时需同时处理其下所有SKU
     * - SKU查询时可关联SPU获取商品基础信息
     * 【注意事项】需确保该ID对应的SPU记录存在
     */
    @Schema(description = "所属SPU")
    private Long productSpuId;

    // ==================== 条码信息 ====================

    /**
     * 主EAN码
     *
     * 【业务含义】European Article Number，欧洲商品编码的13位条码
     * 【数据格式】13位数字，最后一位为校验位
     * 【使用场景】
     * - 零售收银：扫码识别商品
     * - 库存管理：扫描入库出库
     * - 物流追溯：追踪商品流转
     * 【校验规则】EAN-13校验位算法验证
     * 【注意事项】
     * - EAN码在全球范围内需向权威机构申请
     * - 内部测试可使用自定义编码规则
     */
    @Schema(description = "主EAN码(13位)")
    private String productSkuEan;

    // ==================== 规格尺寸信息 ====================

    /**
     * 重量
     *
     * 【业务含义】SKU商品的重量，用于物流计费和配送优化
     * 【数据类型】BigDecimal，保留精确度
     * 【单位】需配合productWeightUnit使用（克、千克等）
     * 【使用场景】
     * - 快递费用计算
     * - 仓库分拣（按重量分类）
     * - 配送路径优化
     */
    @Schema(description = "重量")
    private BigDecimal productWeight;

    /**
     * 重量单位
     *
     * 【业务含义】重量的计量单位
     * 【取值示例】
     * - "g"：克
     * - "kg"：千克
     * - "lb"：磅（英制）
     * - "oz"：盎司（英制）
     * 【默认约定】无特殊说明时默认使用"g"（克）
     */
    @Schema(description = "重量单位")
    private String productWeightUnit;

    /**
     * 长度
     *
     * 【业务含义】SKU商品外包装的长度尺寸
     * 【数据类型】BigDecimal
     * 【单位】厘米（cm）
     * 【使用场景】
     * - 物流体积计算
     * - 仓储货架布局
     * - 包装规格适配
     */
    @Schema(description = "长度(cm)")
    private BigDecimal productLength;

    /**
     * 宽度
     *
     * 【业务含义】SKU商品外包装的宽度尺寸
     * 【数据类型】BigDecimal
     * 【单位】厘米（cm）
     * 【使用场景】同长度字段
     */
    @Schema(description = "宽度(cm)")
    private BigDecimal productWidth;

    /**
     * 高度
     *
     * 【业务含义】SKU商品外包装的高度尺寸
     * 【数据类型】BigDecimal
     * 【单位】厘米（cm）
     * 【使用场景】同长度字段
     */
    @Schema(description = "高度(cm)")
    private BigDecimal productHeight;

    // ==================== 价格信息 ====================

    /**
     * 基准成本价
     *
     * 【业务含义】商品的采购成本基准价，用于成本核算和利润分析
     * 【数据类型】BigDecimal，建议保留2位小数
     * 【定价说明】
     * - 此为含税成本价
     * - 不同供应商供应价格可能不同，此处记录基准价
     * 【使用场景】
     * - 利润计算：（零售价 - 成本价）/ 成本价
     * - 采购决策参考
     * - 财务报表统计
     */
    @Schema(description = "基准成本价")
    private BigDecimal productCostPrice;

    /**
     * 基准零售价
     *
     * 【业务含义】商品的建议零售价格基准
     * 【数据类型】BigDecimal，建议保留2位小数
     * 【定价说明】
     * - 此为建议零售价，实际售价可能因促销活动调整
     * - 会员价、活动价可能低于此价格
     * 【使用场景】
     * - 商品展示参考价
     * - 价格促销对比基准
     * - 价格合理性审核
     */
    @Schema(description = "基准零售价")
    private BigDecimal productRetailPrice;

    // ==================== 图片信息 ====================

    /**
     * SKU主图URL
     *
     * 【业务含义】SKU商品的主图访问地址，用于前端展示
     * 【数据类型】String，完整的HTTP/HTTPS URL
     * 【图片规格建议】
     * - 建议尺寸：800x800像素或更高
     * - 格式：JPG/PNG/WebP
     * - 大小：不超过2MB
     * 【注意事项】
     * - 图片服务应保证高可用性
     * - 建议使用CDN加速访问
     * - 建议添加图片水印防伪
     */
    @Schema(description = "SKU主图URL")
    private String productImageUrl;

    // ==================== 状态信息 ====================

    /**
     * SKU状态
     *
     * 【业务含义】SKU的上架/下架状态，控制商品在前端的展示
     * 【取值范围】
     * - 0：下架（不可销售）
     * - 1：上架（可销售）
     * 【业务规则】
     * - 下架状态时，用户无法购买，但可以查看商品详情
     * - 下架状态时，购物车中的该SKU应提示用户
     * - SPU下所有SKU下架时，SPU也应自动下架
     * 【状态转换】
     * - 上架 -> 下架：手动下架、库存归零自动下架、SPU下架级联
     * - 下架 -> 上架：手动上架、SPU上架不自动上架SKU
     */
    @Schema(description = "状态(0下架1上架)")
    private Integer productSkuStatus;

}
