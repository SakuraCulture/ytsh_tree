package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店主表 DO
 *
 * 【Why】采用主子表设计模式的原因：
 * 1. 门店信息本身相对稳定（名称、地址、区域），而经营状态、空间信息、归属架构等属于动态扩展属性
 * 2. 不同业务场景需要查询不同的属性子集，单表扁平化会导致大量 NULL 字段，影响查询效率和索引利用率
 * 3. 将频繁变更的属性分离到子表，可以避免主表行锁竞争，提升并发写入能力
 * 4. 符合数据库设计范式，降低数据冗余，提高数据一致性
 *
 * 【Why】使用字符串类型 storeId 而非自增主键的原因：
 * 1. 业务上需要展示门店编号（如 "STORE001"），字符串 ID 可以直接作为业务编码使用
 * 2. 支持分布式场景下的 ID 生成（如雪花算法），避免多节点自增 ID 冲突
 * 3. 便于与外部系统对接，外部系统可能使用字符串格式的门店标识
 * 4. 如果将来需要合并不同数据源的门店数据，字符串 ID 更灵活
 *
 * 【Why】使用外键关联而非 JSON 字段的原因：
 * 1. 子表数据需要独立索引查询（如按冷库面积排序、按开业日期筛选）
 * 2. JSON 字段无法建立高效的局部索引，且跨字段统计查询性能差
 * 3. 业务规则变更时，子表结构更容易扩展和维护
 * 4. 便于后续分库分表或数据归档策略的执行
 *
 * 【What】本表负责存储门店的核心基础信息，是业务系统的核心实体之一
 *
 * @author 彼岸花
 */
@TableName("store_table")
@KeySequence("store_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDO extends BaseDO {

    /**
     * 门店ID（业务编码）
     *
     * 【Why】storeId 作为主键的原因：
     * - 门店编号本身具有业务含义，前端展示、报表引用、接口传输都直接使用此字段
     * - 避免了自增主键查询后还需要再查 storeId 的二次查询
     *
     * 【What】门店唯一标识，业务系统中用于关联所有门店相关数据的核心字段
     *
     * 【Constraints】
     * - 必须全局唯一，不允许重复
     * - 建议格式：字母前缀 + 数字序号（如 "STORE001"、"SHOP2024001"）
     * - 最大长度根据业务需求设定，通常不超过 32 位
     * - 创建后不允许随意修改，否则会导致关联数据丢失
     *
     * 【Pitfalls】
     * 【教训2024-03】若在分布式环境中使用自增 ID 而非字符串 ID，可能导致多节点主键冲突
     * 【教训2024-05】修改 storeId 前必须检查所有关联子表和关联表，否则会导致数据孤岛
     */
    @TableId(type = IdType.INPUT)
    private String storeId;

    /**
     * 门店名称
     *
     * 【What】门店的对外展示名称，用于前端显示和用户识别
     *
     * 【Constraints】
     * - 非空，长度建议 2-50 个字符
     * - 同一区域下建议唯一，但不强校验（可能存在连锁店同名情况）
     *
     * 【Pitfalls】
     * - 注意前后端编码一致，建议统一使用 UTF-8
     */
    private String storeName;

    /**
     * 行政区划代码
     *
     * 【Why】使用标准行政区划代码而非文字描述的原因：
     * - 便于与国家统计数据对接，如按省份统计门店分布
     * - 支持层级查询（省-市-区县），通过代码前缀匹配实现
     * - 避免文字歧义（如"朝阳区"在北京和长春都有）
     *
     * 【What】遵循 GB/T 2260 标准，6 位行政区划代码
     *
     * 【Constraints】
     * - 格式：6 位数字代码（如 "110101" 表示北京市朝阳区）
     * - 可为空（门店地址可能在境外或不归属中国行政区划）
     *
     * 【Pitfalls】
     * - 需要定期同步最新行政区划变更（旧代码可能已废止）
     */
    private String regionCode;

    /**
     * 详细地址
     *
     * 【What】门店的具体街道门牌信息，用于地理定位和物流配送
     *
     * 【Constraints】
     * - 最大长度建议不超过 200 字符
     * - 不包含省市区等行政区划信息（已由 regionCode 表示）
     *
     * 【Pitfalls】
     * - 若地址变更但未同步更新 regionCode，会导致区域统计不准确
     */
    private String address;

    /**
     * 门店所属大区
     *
     * 【Why】使用固定枚举而非动态配置的原因：
     * - 大区划分相对稳定，属于组织架构层面的配置
     * - 便于报表统计和 KPI 考核
     *
     * 【What】五级大区体系：EAST=华东、NORTH=华北、SOUTH=华南、WEST=华西、CENTRAL=华中
     *
     * 【Constraints】
     * - 必须使用枚举值之一，不允许自定义
     * - 非空，门店必须归属于某个大区
     *
     * 【Pitfalls】
     * 【教训2023-11】初期设计遗漏了"海外"大区，导致海外门店无法正确归类
     */
    private String area;

    /**
     * 门店状态
     *
     * 【What】控制门店在业务系统中的可用性，状态变更会联动影响订单、配送等业务流程
     *
     * 【Constraints】
     * - 0=停用（不可见、不可下单）、1=正常（可正常运营）
     * - 状态变更建议走审批流程，避免误操作
     * - 停用门店在物理上仍存在，数据不可物理删除
     *
     * 【Pitfalls】
     * - 停用门店时需确保无进行中的订单，否则会导致业务异常
     * - 【教训2024-02】曾因状态变更未通知下游系统，导致已停用门店仍接收订单
     */
    private Integer storeStatus;


}
