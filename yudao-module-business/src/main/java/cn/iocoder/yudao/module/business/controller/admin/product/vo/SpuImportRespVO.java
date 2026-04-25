package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * SPU/SKU/UPC导入操作响应对象
 *
 * 本VO用于返回Excel批量导入商品数据（SPU、SKU、UPC）的执行结果统计。
 *
 * **业务说明：**
 * - 导入操作可能部分成功部分失败，本VO提供完整的执行结果统计
 * - 分别统计SPU、SKU、UPC三种数据类型的成功数量
 * - 记录失败的行号和原因，便于用户定位和修正问题
 *
 * **使用场景：**
 * - 前端显示导入结果汇总
 * - 用户根据失败列表修正Excel后重新导入
 * - 运维人员排查导入异常
 *
 * **设计思路：**
 * - 分离成功/失败计数的优势：
 *   1. 前端可直接展示成功数量，无需再计算
 *   2. 失败列表与成功数量分离，结构更清晰
 *   3. 便于后续扩展（如增加警告列表）
 * - 使用静态内部类ImportFailure的原因：
 *   1. 失败记录是与导入强相关的辅助类，无需单独定义
 *   2. 避免 VO 类过多，增加代码可读性
 *   3. 内部类可以访问外部类字段，符合数据封装原则
 *
 * **潜在隐患及规避建议：**
 * 1. 计数精度问题：成功数量可能因数据关联而重复统计
 *    说明：导入时会根据关联关系判断，如同一SPU下的多个SKU只计一次SPU
 * 2. 失败列表内存占用：大量失败记录可能导致内存压力
 *    规避：建议限制failureList最大长度，超出时截断并提示用户
 * 3. 并发导入场景：多个用户同时导入可能产生数据冲突
 *    建议：使用分布式锁或消息队列保证数据一致性
 *
 * **响应数据示例：**
 * ```json
 * {
 *   "spuSuccessCount": 10,
 *   "skuSuccessCount": 25,
 *   "upcSuccessCount": 40,
 *   "failureList": [
 *     {"row": 5, "message": "SPU编码不能为空"},
 *     {"row": 12, "message": "条码值已存在"}
 *   ]
 * }
 * ```
 *
 */
@Schema(description = "管理后台 - SPU/SKU/UPC导入 Response VO")
@Data
@Builder
public class SpuImportRespVO {

    /**
     * 成功导入的SPU数量
     *
     * 【业务含义】本次导入操作中，成功创建或更新的SPU记录总数
     * 【计数规则】
     * - 新建SPU计为1
     * - 更新SPU（updateSupport=true时）计为1
     * - 重复SPU但未更新（updateSupport=false时）不计入
     * 【使用场景】
     * - 前端展示导入成功提示
     * - 与总行数对比计算成功率
     * - 日志记录和监控统计
     */
    @Schema(description = "成功导入的SPU数量")
    private Integer spuSuccessCount;

    /**
     * 成功导入的SKU数量
     *
     * 【业务含义】本次导入操作中，成功创建或更新的SKU记录总数
     * 【计数规则】
     * - 新建SKU计为1
     * - 更新SKU（updateSupport=true时）计为1
     * - 重复SKU但未更新（updateSupport=false时）不计入
     * 【关联说明】
     * - SKU的创建依赖于有效的SPU
     * - 若SPU创建失败，其下的SKU不会创建
     * - 因此skuSuccessCount <= spuSuccessCount * 平均SKU数
     */
    @Schema(description = "成功导入的SKU数量")
    private Integer skuSuccessCount;

    /**
     * 成功导入的UPC码数量
     *
     * 【业务含义】本次导入操作中，成功创建的UPC条码记录总数
     * 【计数规则】
     * - 只统计新创建的UPC记录
     * - 已存在的UPC不重复创建，也不计入
     * 【关联说明】
     * - UPC的创建依赖于有效的SKU
     * - 若SKU创建失败，其下的UPC不会创建
     * - 因此upcSuccessCount <= skuSuccessCount * 平均UPC数
     */
    @Schema(description = "成功导入的UPC码数量")
    private Integer upcSuccessCount;

    /**
     * 导入失败的记录列表
     *
     * 【业务含义】记录导入过程中发生错误的行及错误原因
     * 【数据说明】
     * - 按行号（row）升序排列，便于用户定位问题
     * - 每条记录包含行号和失败原因
     * - 为空列表表示全部导入成功
     * 【常见失败原因】
     * - "SPU编码不能为空"：必填字段为空
     * - "SPU编码已存在"：重复导入且updateSupport=false
     * - "条码值已存在"：UPC码值唯一性校验失败
     * - "分类ID不存在"：外键关联校验失败
     * 【注意事项】
     * - 失败不影响后续行继续导入，采用"容错导入"策略
     * - 建议在Excel修正失败行后重新导入整文件
     */
    @Schema(description = "导入失败的记录列表")
    private List<ImportFailure> failureList;

    /**
     * 导入失败记录的明细
     *
     * 本内部类用于记录单条导入失败的具体信息。
     *
     * **设计考量：**
     * - 使用静态内部类，因为ImportFailure不依赖外部类的实例字段
     * - 独立@Data和@Builder，因为需要单独序列化
     *
     * @author 芋道源码
     */
    @Data
    @Builder
    public static class ImportFailure {

        /**
         * 失败行号
         *
         * 【业务含义】Excel中失败记录所在的行号（从1开始计数）
         * 【取值说明】对应Excel文件的行号，便于用户定位问题
         * 【注意事项】行号从1开始而非0开始，与用户看到的Excel行号一致
         */
        @Schema(description = "失败行号")
        private Integer row;

        /**
         * 失败原因
         *
         * 【业务含义】导致该行导入失败的具体错误描述
         * 【内容规范】
         * - 应清晰描述错误原因，如"SPU编码不能为空"
         * - 应包含校验规则提示，如"EAN码格式不正确"
         * - 应指出修复方向，如"请检查分类ID是否已创建"
         * 【常见错误类型】
         * - 数据校验失败（空值、格式错误、唯一性冲突）
         * - 业务关联失败（外键不存在）
         * - 数据类型转换失败（数字格式错误）
         * - 系统异常（数据库连接失败等）
         */
        @Schema(description = "失败原因")
        private String message;
    }
}
