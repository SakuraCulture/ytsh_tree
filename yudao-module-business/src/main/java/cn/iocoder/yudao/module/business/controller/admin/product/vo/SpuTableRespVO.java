package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.SkuTableRespVO;

/**
 * SPU基础分类 Response VO
 *
 * 本VO用于返回SPU（标准产品单元）的完整信息，是前端展示商品详情页面的核心数据载体。
 *
 * **业务说明：**
 * - SPU（Standard Product Unit）是商品信息聚合的最小单位
 * - SPU描述的是商品的通用信息，不包含具体销售属性（如颜色、尺寸）
 * - 一个SPU可对应多个SKU（库存量单位），如"iPhone 15 Pro"是一个SPU，"iPhone 15 Pro 256GB 银色"是一个SKU
 *
 * **与SKU的关系：**
 * - SPU是商品的抽象描述，SKU是具体销售单元
 * - SPU包含：商品名称、品牌、产地、厂商、规格模板等基础信息
 * - SKU包含：具体编码、价格、重量、图片等销售属性
 * - 关系：一对多（1个SPU -> N个SKU）
 *
 * **使用场景：**
 * 1. 商品详情页：展示SPU基础信息及其所有可选SKU
 * 2. 商品列表页：展示SPU简要信息供用户浏览
 * 3. 购物车：显示用户选择的SKU所属的SPU信息
 * 4. 订单详情：记录购买的SKU及关联的SPU信息
 * 5. 后台管理：运营人员维护商品主数据
 * 6. 数据导出：导出商品主数据用于分析
 *
 * **字段分组及说明：**
 * | 分组 | 字段 | 说明 |
 * |------|------|------|
 * | 基本信息 | productSpuId, productSpuCode, productSpuName, productBrand | SPU标识和品牌 |
 * | 分类属性 | categoryId | 商品所属分类 |
 * | 供应商属性 | productOrigin, productManufacturer, productSpecTemplate | 产地和规格 |
 * | 图片属性 | productImageUrl, productDetailImages | 商品展示图片 |
 * | 描述属性 | productDescription | 商品详细描述 |
 * | 状态属性 | productSpuStatus | 上下架状态 |
 * | 时间属性 | createTime | 创建时间 |
 * | SKU列表 | skuTables | 关联的SKU列表（嵌套） |
 *
 * **设计思路：**
 * - Response VO与DO分离的原因：
 *   1. API稳定性：数据库字段变更不影响前端接口契约
 *   2. 数据过滤：隐藏内部字段，只返回必要的业务数据
 *   3. 格式化：对时间、金额等进行格式化处理
 * - 包含嵌套SKU列表的原因：
 *   1. 商品详情页通常需要同时展示SPU和所有可选SKU
 *   2. 避免前端因多次请求导致的竞态条件
 *   3. 减少HTTP请求次数，提升页面加载性能
 * - 使用@ExcelProperty注解的原因：
 *   1. 支持将SPU数据导出为Excel文件
 *   2. 与导入模板保持一致的表头命名
 *
 * **潜在隐患及规避建议：**
 * 1. 嵌套SKU数据量：当SPU下SKU数量很多时，嵌套返回会导致单次响应数据量过大
 *    规避：限制返回的SKU数量，或提供独立的SKU查询接口
 * 2. 图片URL失效：productImageUrl和productDetailImages为外部URL，可能过期或变更
 *    建议：使用CDN或图片服务，确保URL长期有效
 * 3. 分类ID孤立：categoryId只返回ID，前端需单独查询分类名称
 *    建议：在Service层关联查询分类表，返回分类名称字段
 * 4. 详情图片格式：productDetailImages格式不统一（JSON/逗号分隔）
 *    建议：统一图片格式，在数据入库时进行标准化处理
 *
 * **响应数据示例：**
 * ```json
 * {
 *   "productSpuId": 20081,
 *   "productSpuCode": "SPU001",
 *   "productSpuName": "iPhone 15 Pro",
 *   "productBrand": "Apple",
 *   "categoryId": 10001,
 *   "productOrigin": "中国",
 *   "productManufacturer": "苹果电子产品公司",
 *   "productSpecTemplate": "手机规格模板",
 *   "productImageUrl": "https://cdn.example.com/spu/20081/main.jpg",
 *   "productDetailImages": "https://cdn.example.com/spu/20081/detail1.jpg,https://cdn.example.com/spu/20081/detail2.jpg",
 *   "productDescription": "iPhone 15 Pro采用钛金属设计，配备A17 Pro芯片...",
 *   "productSpuStatus": 1,
 *   "createTime": "2024-01-15 10:30:00",
 *   "skuTables": [
 *     { "productSkuId": 30001, "productSkuCode": "IP15P-256-S", "productSkuName": "iPhone 15 Pro 256GB 银色", ... },
 *     { "productSkuId": 30002, "productSkuCode": "IP15P-256-G", "productSkuName": "iPhone 15 Pro 256GB 灰色", ... }
 *   ]
 * }
 * ```
 *
 */
@Schema(description = "管理后台 - SPU基础分类 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class SpuTableRespVO {

    // ==================== SPU基本信息字段 ====================

    /**
     * SPU ID
     *
     * 【业务含义】SPU记录的主键，唯一标识一个SPU
     * 【数据类型】Long，长整型
     * 【取值说明】由数据库自增生成，全球唯一
     * 【使用场景】
     * - SPU查询和更新的操作依据
     * - SPU与SKU关联的外键
     * - 数据统计和日志记录的主键
     */
    @Schema(description = "SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20081")
    @ExcelProperty("SPU ID")
    private Long productSpuId;

    /**
     * SPU编码
     *
     * 【业务含义】SPU的业务编码，用于业务人员识别和检索
     * 【必填性】建议必填，用于快速定位SPU
     * 【编码规则】
     * - 通常包含分类、业务含义等前缀
     * - 建议全局唯一，避免重复
     * 【示例】"SPU-IPHONE15PRO"、"SPU-COKE330"
     */
    @Schema(description = "SPU编码")
    @ExcelProperty("SPU编码")
    private String productSpuCode;

    /**
     * SPU名称
     *
     * 【业务含义】商品的标准产品单元名称，即商品的通用名称
     * 【命名规范】应描述商品的本质特征，不包含具体销售属性
     * 【示例】
     * - 正确："iPhone 15 Pro"（通用名称）
     * - 错误："iPhone 15 Pro 256GB 银色"（包含SKU属性）
     */
    @Schema(description = "SPU名称", example = "芋艿")
    @ExcelProperty("SPU名称")
    private String productSpuName;

    /**
     * 品牌
     *
     * 【业务含义】商品所属品牌名称
     * 【使用场景】
     * - 品牌筛选和搜索
     * - 品牌商品统计
     * - 品牌授权验证
     * 【示例】"Apple"、"华为"、"可口可乐"
     */
    @Schema(description = "品牌")
    @ExcelProperty("品牌")
    private String productBrand;

    // ==================== SPU分类属性字段 ====================

    /**
     * 分类ID
     *
     * 【业务含义】商品所属分类的主键ID
     * 【数据类型】Long
     * 【关联说明】外键关联分类表，支持层级分类（如一级分类 -> 二级分类 -> 三级分类）
     * 【使用场景】
     * - 分类筛选（如查看手机类商品）
     * - 分类统计数据展示
     * - 分类导航
     * 【注意事项】只返回ID，分类名称需关联查询获得
     */
    @Schema(description = "分类ID", example = "27572")
    @ExcelProperty("分类ID")
    private Long categoryId;

    // ==================== SPU供应商属性字段 ====================

    /**
     * 产地
     *
     * 【业务含义】商品的生产地或原产地
     * 【使用场景】
     * - 进口商品标识
     * - 产地追溯
     * - 关税计算依据
     * 【示例】"中国"、"日本"、"美国"、"德国"
     */
    @Schema(description = "产地")
    @ExcelProperty("产地")
    private String productOrigin;

    /**
     * 生产商
     *
     * 【业务含义】商品的生产制造企业名称
     * 【使用场景】
     * - 供应商管理
     * - 厂商商品统计
     * - 质量追溯
     * 【示例】"苹果电子产品公司"、"可口可乐公司"、"华为技术有限公司"
     */
    @Schema(description = "生产商")
    @ExcelProperty("生产商")
    private String productManufacturer;

    /**
     * 规格模板
     *
     * 【业务含义】商品规格的参数模板名称，用于标准化商品规格描述
     * 【使用场景】
     * - 规格标准化管理
     * - 同模板商品归类
     * - 规格参数自动填充
     * 【示例】"手机规格模板"、"饮料规格模板"、"服装规格模板"
     */
    @Schema(description = "规格模板")
    @ExcelProperty("规格模板")
    private String productSpecTemplate;

    // ==================== SPU图片属性字段 ====================

    /**
     * 商品主图URL
     *
     * 【业务含义】SPU主图的访问地址，用于列表页展示和分享
     * 【数据类型】String，完整的HTTP/HTTPS URL
     * 【图片规格建议】
     * - 建议尺寸：800x800像素或更高
     * - 格式：JPG/PNG/WebP
     * - 大小：不超过2MB
     * 【使用场景】
     * - 商品列表页缩略图
     * - 社交分享预览图
     * - 购物车商品图标
     * 【注意事项】
     * - 图片服务应保证高可用性
     * - 建议使用CDN加速访问
     * - 建议添加图片水印防伪
     */
    @Schema(description = "商品主图URL", example = "https://www.iocoder.cn")
    @ExcelProperty("商品主图URL")
    private String productImageUrl;

    /**
     * 商品详情图片
     *
     * 【业务含义】SPU详情页的图片组，用于展示商品详细信息
     * 【数据格式】
     * - 逗号分隔的URL列表："url1,url2,url3"
     * - 或JSON数组字符串："[url1,url2,url3]"
     * 【使用场景】
     * - 商品详情页轮播图
     * - 商品描述中的插图
     * - 质量检测报告图片
     * 【注意事项】
     * - 图片顺序影响展示顺序
     * - 建议控制图片数量（通常5-10张）
     * - 建议使用CDN加速访问
     */
    @Schema(description = "商品详情图片")
    @ExcelProperty("商品详情图片")
    private String productDetailImages;

    // ==================== SPU描述属性字段 ====================

    /**
     * 商品描述
     *
     * 【业务含义】商品的详细描述文本，支持富文本内容（HTML）
     * 【数据格式】纯文本或HTML富文本
     * 【使用场景】
     * - 商品详情页主要内容
     * - 商品搜索的文本匹配
     * - SEO优化（meta描述）
     * 【注意事项】
     * - HTML内容需在前端进行XSS过滤
     * - 图片建议使用CDN托管的URL
     * - 建议限制描述长度，避免影响页面加载
     */
    @Schema(description = "商品描述", example = "随便")
    @ExcelProperty("商品描述")
    private String productDescription;

    // ==================== SPU状态属性字段 ====================

    /**
     * SPU状态
     *
     * 【业务含义】商品的上架/下架状态，控制商品在前端的展示
     * 【取值范围】
     * - 0：下架（不可销售）
     * - 1：上架（可销售）
     * 【业务规则】
     * - 上架商品才能被用户购买
     * - 下架商品保留数据但不影响前端展示
     * - SPU下架不影响其SKU的独立状态
     * 【状态转换】
     * - 上架 -> 下架：手动下架、库存归零自动下架
     * - 下架 -> 上架：手动上架
     */
    @Schema(description = "状态(0下架1上架)", example = "2")
    @ExcelProperty("状态(0下架1上架)")
    private Integer productSpuStatus;

    // ==================== SPU时间属性字段 ====================

    /**
     * 创建时间
     *
     * 【业务含义】SPU记录的创建时间
     * 【数据类型】LocalDateTime
     * 【使用场景】
     * - 按创建时间排序
     * - 数据统计（如按月统计新增商品数）
     * - 操作日志追溯
     * 【日期格式】yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    // ==================== SPU关联SKU列表字段 ====================

    /**
     * SKU商品主数据列表
     *
     * 【业务含义】该SPU下的所有SKU列表，用于展示商品的具体销售单元
     * 【数据类型】List<SkuTableRespVO>
     * 【业务规则】
     * - 一个SPU可以对应0到多个SKU
     * - SKU按创建时间或自定义规则排序
     * - SKU列表包含该SPU的所有款式/规格变体
     * 【使用场景】
     * - 商品详情页展示所有可选SKU（如颜色、尺寸）
     * - SKU选择组件的数据源
     * - 库存管理界面
     * 【设计原因】
     * - 嵌套返回而非独立接口，因为商品详情页通常需要同时展示SPU和SKU
     * - 避免前端因多次请求导致的竞态条件
     * - 减少HTTP请求次数，提升页面加载性能
     * 【注意事项】
     * - 当SKU数量很多时，可能影响响应速度
     * - 建议限制返回的SKU数量，或提供分页支持
     * - 返回空列表表示该SPU暂无SKU，而非错误
     */
    @Schema(description = "SKU商品主数据列表")
    private List<SkuTableRespVO> skuTables;

}
