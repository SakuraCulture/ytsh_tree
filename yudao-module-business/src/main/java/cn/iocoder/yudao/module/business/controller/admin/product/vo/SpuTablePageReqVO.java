package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * SPU基础分类分页查询 Request VO
 *
 * 本VO用于分页查询SPU（标准产品单元）列表，支持多条件组合筛选。
 *
 * **业务说明：**
 * - SPU（Standard Product Unit）是商品信息聚合的最小单位
 * - 本VO继承PageParam，获得分页能力（pageNo、pageSize）
 * - 支持按SPU自身属性筛选，也支持按关联的SKU属性筛选
 *
 * **筛选维度：**
 * 1. SPU基础属性：编码、名称、品牌、产地、厂商、规格模板
 * 2. 分类属性：分类ID
 * 3. 图片属性：主图URL、详情图片
 * 4. 描述属性：商品描述
 * 5. 状态属性：上下架状态
 * 6. 时间属性：创建时间范围
 * 7. SKU关联属性：SKU编码、SKU名称（用于查找有特定SKU的SPU）
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | SPU基本信息 | productSpuCode, productSpuName, productBrand | 基础识别信息 |
 * | SPU属性 | productOrigin, productManufacturer, productSpecTemplate | 供应商和规格 |
 * | 分类属性 | categoryId | 商品所属分类 |
 * | 图片属性 | productImageUrl, productDetailImages | 商品展示图片 |
 * | 描述属性 | productDescription | 商品详细描述 |
 * | 状态属性 | productSpuStatus | 上下架状态 |
 * | 时间属性 | createTime | 创建时间范围 |
 * | SKU关联 | productSkuCode, productSkuName | 关联SKU的筛选条件 |
 *
 * **设计思路：**
 * - 继承PageParam的原因：
 *   1. 复用分页参数，避免重复定义pageNo、pageSize字段
 *   2. 符合面向对象设计原则，减少样板代码
 * - 包含SKU筛选字段的原因：
 *   1. 支持"查找有特定SKU的SPU"场景，如查找有"白色款"SKU的商品
 *   2. 减少前端多次请求，直接通过SPU分页接口筛选
 * - 时间字段使用数组的原因：
 *   1. 支持时间范围查询，数组第一个元素为开始时间，第二个为结束时间
 *   2. 符合MyBatis-Plus的-between查询习惯
 *
 * **使用场景：**
 * 1. 后台商品管理列表页：运营人员筛选和查找商品
 * 2. 商品上下架批量操作
 * 3. 商品数据导出（配合导出接口）
 * 4. 品牌/分类筛选后的商品列表
 *
 * **潜在隐患及规避建议：**
 * 1. SKU关联查询性能：联合SKU表查询时，数据量大可能导致性能问题
 *    规避：确保SKU表的productSkuCode和productSkuName有索引
 * 2. 时间范围查询：createTime为空数组时查询逻辑需特殊处理
 *    建议：在Mapper层处理空数组情况，避免无效SQL
 * 3. 模糊查询性能：多个LIKE查询同时使用可能较慢
 *    建议：限制模糊查询字段数量，或使用全文索引
 *
 */
@Schema(description = "管理后台 - SPU基础分类分页 Request VO")
@Data
public class SpuTablePageReqVO extends PageParam {

    // ==================== SPU基础信息字段 ====================

    /**
     * SPU编码
     *
     * 【业务含义】商品的标准产品单元编码，用于唯一标识和检索
     * 【筛选方式】精确匹配或模糊匹配（根据业务需求）
     * 【使用场景】
     * - 后台商品管理按编码搜索
     * - ERP系统对接按编码同步
     */
    @Schema(description = "SPU编码")
    private String productSpuCode;

    /**
     * SPU名称
     *
     * 【业务含义】商品的标准产品单元名称
     * 【筛选方式】模糊匹配，支持按名称关键词搜索
     * 【使用场景】
     * - 用户搜索商品名称
     * - 后台按名称筛选商品
     * 【示例】输入"iPhone"可匹配"iPhone 15 Pro"、"iPhone 15 Plus"
     */
    @Schema(description = "SPU名称", example = "芋艿")
    private String productSpuName;

    /**
     * 品牌
     *
     * 【业务含义】商品所属品牌名称
     * 【筛选方式】精确匹配或模糊匹配
     * 【使用场景】
     * - 品牌筛选器
     * - 品牌商品统计
     * 【示例】"Apple"、"华为"、"可口可乐"
     */
    @Schema(description = "品牌")
    private String productBrand;

    // ==================== SPU分类属性字段 ====================

    /**
     * 分类ID
     *
     * 【业务含义】商品所属分类的主键ID
     * 【数据类型】Long
     * 【筛选方式】精确匹配
     * 【关联说明】外键关联分类表，支持层级分类筛选
     * 【使用场景】
     * - 分类筛选（如查看手机类商品）
     * - 分类统计数据展示
     */
    @Schema(description = "分类ID", example = "27572")
    private Long categoryId;

    // ==================== SPU供应商属性字段 ====================

    /**
     * 产地
     *
     * 【业务含义】商品的生产地或原产地
     * 【筛选方式】精确匹配或模糊匹配
     * 【使用场景】
     * - 进口商品筛选
     * - 产地追溯
     * 【示例】"中国"、"日本"、"德国"
     */
    @Schema(description = "产地")
    private String productOrigin;

    /**
     * 生产商
     *
     * 【业务含义】商品的生产制造企业名称
     * 【筛选方式】精确匹配或模糊匹配
     * 【使用场景】
     * - 供应商管理
     * - 厂商商品统计
     * 【示例】"苹果电子产品公司"、"可口可乐公司"
     */
    @Schema(description = "生产商")
    private String productManufacturer;

    /**
     * 规格模板
     *
     * 【业务含义】商品规格的参数模板名称，用于标准化规格描述
     * 【筛选方式】精确匹配
     * 【使用场景】
     * - 规格标准化管理
     * - 同模板商品归类
     * 【示例】"手机规格模板"、"饮料规格模板"
     */
    @Schema(description = "规格模板")
    private String productSpecTemplate;

    // ==================== SPU图片属性字段 ====================

    /**
     * 商品主图URL
     *
     * 【业务含义】SPU主图的访问地址
     * 【筛选说明】通常不作为筛选条件，此处预留
     * 【使用场景】
     * - 图片链接导出
     * - CDN预热触发
     */
    @Schema(description = "商品主图URL", example = "https://www.iocoder.cn")
    private String productImageUrl;

    /**
     * 商品详情图片
     *
     * 【业务含义】SPU详情页的图片组，多张图片用逗号分隔
     * 【数据格式】JSON数组字符串或逗号分隔的URL列表
     * 【筛选说明】通常不作为筛选条件
     */
    @Schema(description = "商品详情图片")
    private String productDetailImages;

    // ==================== SPU描述属性字段 ====================

    /**
     * 商品描述
     *
     * 【业务含义】商品的详细描述文本，支持富文本内容
     * 【筛选方式】模糊匹配
     * 【使用场景】
     * - 商品详情搜索
     * - 关键词命中统计
     * 【注意事项】模糊匹配描述字段可能影响查询性能
     */
    @Schema(description = "商品描述", example = "随便")
    private String productDescription;

    // ==================== SPU状态属性字段 ====================

    /**
     * SPU状态
     *
     * 【业务含义】商品的上架/下架状态
     * 【取值范围】
     * - 0：下架（不可销售）
     * - 1：上架（可销售）
     * - 2或其他：全部（含上下架）
     * 【筛选说明】值为2或null时表示查询全部状态
     * 【业务规则】
     * - 上架商品才能被用户购买
     * - 下架商品保留数据但不影响前端展示
     */
    @Schema(description = "状态(0下架1上架)", example = "2")
    private Integer productSpuStatus;

    // ==================== 时间属性字段 ====================

    /**
     * 创建时间
     *
     * 【业务含义】SPU记录的创建时间范围
     * 【数据类型】LocalDateTime数组，长度为2
     * 【数组说明】
     * - 第一个元素（[0]）：开始时间，包含边界
     * - 第二个元素（[1]）：结束时间，包含边界
     * - 为空数组或null：表示不限时间范围
     * 【筛选方式】BETWEEN查询，闭区间
     * 【使用场景】
     * - 按创建时间排序
     * - 时间范围筛选（如查看某时间段内创建的商品）
     * - 数据统计（如按月统计新增商品数）
     * 【日期格式】yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    // ==================== SKU关联属性字段（联合查询） ====================

    /**
     * SKU编码
     *
     * 【业务含义】关联的SKU编码，用于筛选有特定SKU的SPU
     * 【筛选方式】模糊匹配
     * 【使用场景】
     * - 查找有特定款式SKU的商品（如"白色款"）
     * - SPU/SKU联合筛选
     * 【关联说明】
     * - 此字段会触发SPU表与SKU表的联合查询
     * - 返回所有拥有匹配SKU的SPU列表
     * 【性能注意】联合查询时确保SKU表有合适索引
     */
    @Schema(description = "SKU编码")
    private String productSkuCode;

    /**
     * SKU名称
     *
     * 【业务含义】关联的SKU名称，用于筛选有特定款式名称的SPU
     * 【筛选方式】模糊匹配
     * 【使用场景】
     * - 查找有特定规格名称的商品（如"256G"、"XL码"）
     * - 颜色/尺寸筛选
     * 【关联说明】
     * - 此字段会触发SPU表与SKU表的联合查询
     * - 支持跨SPU的SKU名称搜索
     */
    @Schema(description = "SKU名称", example = "芋艿")
    private String productSkuName;

    /**
     * SPU ID列表（内部用于SKU关联查询）
     */
    @Schema(description = "SPU ID列表（内部使用）")
    private List<Long> spuIds;

}
