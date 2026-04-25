package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店经营状态 DO
 *
 * 【Why】单独存储经营状态信息的意义：
 * 1. 经营状态数据（当前状态、开业日期、签约日期）属于时间维度的事件型数据，与静态属性分离更合理
 * 2. 开业日期和签约日期是重要的业务里程碑，需要独立记录便于历史追溯和统计
 * 3. 当前状态是动态属性，变更频率较高，单独存储可减少主表锁竞争
 * 4. 便于按时间维度进行经营分析，如计算门店存活率、平均签约到开业周期等
 *
 * 【What】本表负责存储门店的经营状态信息，包括当前状态、开业日期和签约日期
 *
 * @author 彼岸花
 */
@TableName("store_business_status_table")
@KeySequence("store_business_status_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessStatusTableDO extends BaseDO {

    /**
     * 经营状态ID
     *
     * 【What】经营状态记录的唯一标识
     *
     * 【Constraints】
     * - 自增主键，确保唯一性
     * - 与 storeId 组合唯一
     *
     * 【Pitfalls】
     * - 需确保与 storeId 是一对一关系，防止同一门店出现多条状态记录
     */
    @TableId
    private Long storeBusinessStatusId;

    /**
     * 门店ID
     *
     * 【What】关联到 store_table.store_id，标识这条状态信息属于哪个门店
     *
     * 【Constraints】
     * - 非空，必须指向已存在的门店
     *
     * 【Pitfalls】
     * - 需与门店主表建立外键关联
     */
    private String storeId;

    /**
     * 当前状态
     *
     * 【Why】使用枚举值控制门店业务状态的原因：
     * - 当前状态直接影响业务流转（接单、配送、结算等），需要严格控制
     * - 便于通过状态机实现业务流程控制
     * - 支持状态变更的历史记录和审计
     *
     * 【What】枚举值含义：
     * - NORMAL：正常营业（门店已通过验收，可以正常接收订单和开展业务）
     * - CLOSED：已关闭（门店暂时或永久停止营业，不再接收新订单）
     *
     * 【Constraints】
     * - 必须使用上述枚举值之一，不允许自定义
     * - 非空，门店必须有明确的当前状态
     * - 状态从 NORMAL 变为 CLOSED 通常需要走审批流程
     *
     * 【Pitfalls】
     * 【教训2024-07】曾因状态枚举值不足，新增状态时未考虑与原有业务流程的兼容性，导致订单状态与门店状态不一致
     * - 状态枚举变更需评估对现有业务流程的影响
     * - 建议在状态变更时触发业务事件通知相关系统
     */
    private String currentStatus;

    /**
     * 开业日期
     *
     * 【Why】使用 LocalDate 而非 LocalDateTime 的原因：
     * - 开业日期只需精确到"天"，门店开业通常在某个日期，而非某个具体时刻
     * - 使用 Date 类型可以避免时间存储的歧义（如时区、夏令时）
     *
     * 【Why】单独存储开业日期的原因：
     * - 开业日期是重要的业务里程碑，用于计算门店店龄、统计月度/年度开业门店数
     * - 便于进行开业 Anniversary 营销活动（如店庆促销）
     * - 支持按开业时长进行门店分级（如新店、老店）
     *
     * 【What】门店正式对外营业的日期，通常是验收通过后的第一天
     *
     * 【Constraints】
     * - 格式：日期（YYYY-MM-DD）
     * - 必须小于等于当前日期（不允许预填未来日期）
     * - 通常晚于或等于 signDate（签约后才能开业）
     * - 可为空（新开业门店尚未确定开业日期）
     *
     * 【Pitfalls】
     * 【教训2024-08】曾因开业日期与验收通过日期混淆，导致统计数据失真
     * - 需明确"开业日期"的定义，与业务方达成一致
     * - 前端录入时应提供日期选择器，避免手动输入格式错误
     */
    private LocalDate openDate;

    /**
     * 签约日期
     *
     * 【Why】区分签约日期和开业日期的原因：
     * - 签约和开业是独立的业务里程碑，时间跨度可能很长（装修期、资质审批等）
     * - 签约日期用于计算签约到开业的周期，评估开店效率
     * - 便于按签约时间进行财务核算（如按年统计应付款）
     * - 签约日期是法律意义上的合同开始日期，与实际运营日期需区分
     *
     * 【What】门店与公司签订合作合同的日期，代表合作关系的正式建立
     *
     * 【Constraints】
     * - 格式：日期（YYYY-MM-DD）
     * - 必须小于等于 openDate（逻辑上先签约后开业）
     * - 可为空（如早期门店数据未记录签约日期）
     *
     * 【Pitfalls】
     * 【教训2023-09】早期数据中 signDate 字段缺失较多，导致无法准确计算签约到开业周期
     * - 建议在数据迁移时标注缺失原因，便于后续数据清洗
     * - 前端录入时可提示"签约日期通常早于开业日期"
     */
    private LocalDate signDate;

}
