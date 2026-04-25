package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店空间信息 DO
 *
 * 【Why】单独存储空间信息的意义：
 * 1. 门店面积数据与经营属性（状态、归属）无直接关联，属于物理资产信息
 * 2. 便于按照面积维度进行统计和分析，如计算平均门店面积、冷库配置率等
 * 3. 面积数据可能需要定期复测更新，独立存储便于版本管理
 * 4. 支持按面积阈值进行门店筛选（如筛选冷库面积 > 100㎡ 的门店）
 *
 * 【What】本表负责存储门店的空间物理属性信息，包括建筑面积和冷库面积
 *
 * @author 彼岸花
 */
@TableName("store_space_table")
@KeySequence("store_space_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTableDO extends BaseDO {

    /**
     * 空间信息ID
     *
     * 【What】空间信息记录的唯一标识
     *
     * 【Constraints】
     * - 自增主键，确保唯一性
     * - 与 storeId 组合唯一（一个门店只有一条空间信息记录）
     *
     * 【Pitfalls】
     * - 需确保与 storeId 是一对一关系，代码层应做校验防止重复数据
     */
    @TableId
    private Long storeSpaceId;

    /**
     * 门店ID
     *
     * 【Why】使用外键关联而非冗余门店名称的原因：
     * - 数据一致性：避免门店名称变更后需要同步更新多处
     * - 节省存储：空间表只存储必要的 storeId，门店主表存储完整信息
     * - 规范化设计：符合数据库设计范式，减少数据冗余
     *
     * 【What】关联到 store_table.store_id，标识这条空间信息属于哪个门店
     *
     * 【Constraints】
     * - 非空，必须指向已存在的门店
     * - 建议在应用层或数据库层建立外键约束
     *
     * 【Pitfalls】
     * 【教训2024-01】曾因未建立外键约束，删除了门店主表数据后，空间表成为孤立数据
     */
    private String storeId;

    /**
     * 建筑面积（平方米）
     *
     * 【Why】使用 BigDecimal 而非 Double 的原因：
     * - 避免浮点数精度丢失（如 0.1 + 0.2 != 0.3）
     * - 面积数据可能涉及小数（如 123.5㎡），需要精确计算
     *
     * 【What】门店占用场地的总建筑面积，单位：平方米（㎡）
     *
     * 【Constraints】
     * - 单位：平方米（㎡），存储数值而非字符串
     * - 取值范围：大于 0，通常不超过 100000（约 10 公顷）
     * - 精度：小数点后保留 2 位
     * - 可为空（门店场地信息未知或未测量）
     *
     * 【Pitfalls】
     * 【教训2023-08】早期使用 String 存储面积，导致无法进行数值排序和统计计算
     * - 应用层需统一约定单位，前端输入和后端存储保持一致
     * - 数据导入时需校验单位是否统一（如有些数据可能是"平方英尺"）
     */
    private BigDecimal buildingArea;

    /**
     * 冷库面积（平方米）
     *
     * 【Why】单独存储冷库面积的原因：
     * - 冷库是特殊资产，涉及食品安全和合规监管，需要单独统计
     * - 便于计算冷链覆盖率、冷库配置率等运营指标
     * - 支持按冷库面积门槛进行合规检查（如某些资质需要冷库面积 > 50㎡）
     *
     * 【What】门店配备的冷链仓储空间，单位：平方米（㎡）
     *
     * 【Constraints】
     * - 单位：平方米（㎡）
     * - 取值范围：大于等于 0（即允许门店无冷库）
     * - 精度：小数点后保留 2 位
     * - 可为空，表示门店尚未配置冷库
     * - 通常不超过 buildingArea（冷库面积不可能大于总建筑面积）
     *
     * 【Pitfalls】
     * 【教训2024-06】曾因未校验冷库面积与建筑面积的关系，导致数据显示冷库面积 > 总面积
     * - 建议在数据库层或应用层添加 CHECK 约束或校验逻辑
     * - 前端录入时应提供友好提示，如"冷库面积不能超过建筑面积"
     */
    private BigDecimal coldStorageArea;

}
