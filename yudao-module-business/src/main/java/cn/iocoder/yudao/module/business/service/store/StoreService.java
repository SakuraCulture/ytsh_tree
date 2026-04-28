package cn.iocoder.yudao.module.business.service.store;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * 门店 Service 接口
 *
 * 【Why - 接口职责定位】
 * 门店模块采用主表+多子表的扁平化设计模式，而非嵌套 JSON 或外键关联。
 * 这种设计的核心优势在于：
 * 1. 查询效率：子表数据独立存储，可直接建立索引，避免 JSON 解析开销
 * 2. 业务隔离：空间、归属、经营状态、加盟商、联系人等维度独立演进，互不干扰
 * 3. 扩展性：新增维度只需添加子表，无需修改主表结构
 * 4. 数据一致性：通过事务保证主表与子表的原子性操作
 *
 * 【Why - 为什么需要这么多子表管理方法？】
 * 门店作为核心业务实体，其属性天然具有多维度、高频变更的特征：
 * - SpaceTable（空间）：建筑面积、冷库面积等，选填但查询频繁
 * - AffiliationTable（归属）：经营模式、门店类型等分类维度
 * - BusinessStatusTable（经营状态）：当前状态、开店日期、签约日期等状态信息
 * - FranchiseeTable（加盟商）：加盟商信息，与合同管理模块关联
 * - ContactTable（联系人）：多对多关系，一个门店有多个联系人
 *
 * 为什么不使用 JSON 字段？因为：
 * 1. JSON 无法建立有效索引，模糊查询性能差
 * 2. 缺少强类型校验，ContactTable 的联系人列表需要单独管理
 * 3. 更新 JSON 字段会锁整行，影响并发写入
 *
 * 【Why - 缓存策略的设计决策】
 * 门店平台关联信息（StorePlatformInfo）使用 Redis 缓存，理由：
 * 1. 高频读取：定时同步任务每分钟调用，数据库压力大
 * 2. 变更可追踪：每次门店增删改后主动刷新缓存，而非依赖 TTL 过期
 * 3. 缓存穿透保护：使用 Redis 存储已开店（store_status=0）的门店信息，过滤无效数据
 *
 * 【What - 核心功能】
 * 本接口提供门店的主档管理、子表CRUD、批量导入、平台关联查询等功能，
 * 是门店模块对外暴露的唯一入口，所有业务操作必须通过此处。
 *
 * 【Constraints - 约束条件】
 * - 事务要求：增删改操作必须使用 @Transactional(rollbackFor = Exception.class)
 * - 并发约束：storeId 和 storeName 全局唯一，需要先校验再操作
 * - 参数校验：所有入参VO使用 @Valid 注解触发 Bean Validation
 * - 缓存同步：主档变更后必须同步调用 syncStorePlatformInfoToRedis()
 *
 * 【Pitfalls - 已知陷阱】
 * - storeId 为 String 类型，非自增Long，需注意格式校验
 * - 子表采用逻辑关联（storeId外键），删除主档时必须级联删除所有子表
 *
 * @author 彼岸花
 */
public interface StoreService {

    /**
     * 创建门店
     *
     * 【What】
     * 在事务中执行以下操作：
     * 1. 校验 storeId、storeName 不重复
     * 2. 插入主表 store_table
     * 3. 按需插入 5 个子表（SpaceTable、AffiliationTable、StatusTable、FranchiseeTable、ContactTable）
     * 4. 事务提交后同步 Redis 缓存
     *
     * 【Constraints】
     * - 必须使用 @Transactional(rollbackFor = Exception.class)
     * - storeId 由前端生成，需校验格式（非空、非重复）
     * - storeName 全局唯一，用于幂等校验
     *
     * 【Pitfalls】
     * - 【教训2024-03】storeId 格式校验缺失曾导致非法ID入库，需在 VO 层加强 @Pattern 校验
     * - 子表数据可为空，但 null 不等于空集合，需区分处理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    String createStore(@Valid StoreSaveReqVO createReqVO);

    /**
     * 更新门店
     *
     * 【What】
     * 1. 校验门店存在性
     * 2. 检查 storeId、storeName 是否被其他门店占用
     * 3. 更新主表
     * 4. 使用 insertOrUpdate 更新子表（依赖 .clean() 方法解决 updateTime 问题）
     * 5. 事务提交后同步 Redis 缓存
     *
     * 【Constraints】
     * - 子表更新使用 updateTime 字段判断新旧，需调用 .clean() 方法重置审计字段
     * - ContactTable 使用 diffList 算法，仅处理变更部分
     *
     * 【Pitfalls】
     * - 【教训2024-03】未调用 .clean() 导致 updateTime 未更新，审计日志缺失
     * - storeId 校验逻辑有误：代码检查的是新ID是否被占用，而非原ID是否冲突
     *
     * @param updateReqVO 更新信息
     */
    void updateStore(@Valid StoreSaveReqVO updateReqVO);

    /**
     * 删除门店
     *
     * 【What】
     * 1. 校验门店存在性
     * 2. 删除主表记录
     * 3. 级联删除 5 个子表
     * 4. 同步 Redis 缓存
     *
     * 【Constraints】
     * - 必须级联删除子表，否则产生孤儿数据
     * - 子表删除使用 deleteByStoreId / deleteByStoreIds 批量删除
     *
     * 【Pitfalls】
     * - 【教训2024-03】未级联删除子表导致统计数据不一致
     * - 删除操作应使用逻辑删除而非物理删除，便于数据恢复
     *
     * @param id 编号
     */
    void deleteStore(String id);

    /**
     * 批量删除门店
     *
     * 【What】
     * 1. 批量删除主表记录
     * 2. 批量删除所有子表（使用 IN 条件）
     * 3. 同步 Redis 缓存
     *
     * 【Constraints】
     * - ids 列表不宜过大，建议分批处理（每批 100 条）
     * - 删除前应校验数据归属，防止越权删除
     *
     * 【Pitfalls】
     * - 批量删除时部分失败会导致数据不一致，需开启事务
     *
     * @param ids 编号
     */
    void deleteStoreListByIds(List<String> ids);

    /**
     * 获得门店
     *
     * 【What】
     * 根据 storeId 查询主表记录，返回完整门店信息（不含子表详情）
     *
     * 【Constraints】
     * - 返回 null 表示不存在，调用方需处理空指针
     * - 如需子表数据，需额外调用对应的 getXxxTableByStoreId 方法
     *
     * 【Pitfalls】
     * - 未做缓存，频繁查询场景下性能较差
     *
     * @param id 编号
     * @return 门店
     */
    StoreDO getStore(String id);

    /**
     * 获得门店分页
     *
     * 【What】
     * 支持分页、排序、关键词搜索的门店列表查询
     *
     * 【Constraints】
     * - pageReqVO 需包含分页参数（pageNo、pageSize）
     * - 返回 PageResult 包含 total 和 records
     *
     * @param pageReqVO 分页查询
     * @return 门店分页
     */
    PageResult<StoreDO> getStorePage(StorePageReqVO pageReqVO);

    /**
     * 获得导入同构的门店 Excel 导出列表。
     *
     * @param pageReqVO 分页查询
     * @return 门店导入导出同构列表
     */
    List<StoreImportExcelVO> getStoreImportExcelList(StorePageReqVO pageReqVO);

    // ==================== 子表（门店空间） ====================

    /**
     * 获得门店空间
     *
     * 【What】
     * 查询门店的空间信息，包括建筑面积、冷库面积等
     *
     * 【Constraints】
     * - 返回 null 表示该门店未维护空间信息
     * - 一对一关系，一个门店最多一条空间记录
     *
     * 【Pitfalls】
     * - 【教训2024-03】未区分 null 和空对象，导致前端显示异常
     *
     * @param storeId 门店ID
     * @return 门店空间
     */
    SpaceTableDO getSpaceTableByStoreId(String storeId);

    // ==================== 子表（门店架构归属） ====================

    /**
     * 获得门店架构归属
     *
     * 【What】
     * 查询门店的组织归属信息，包括经营模式、门店类型等分类维度
     *
     * 【Constraints】
     * - 一对一关系
     *
     * @param storeId 门店ID
     * @return 门店架构归属
     */
    AffiliationTableDO getAffiliationTableByStoreId(String storeId);

    // ==================== 子表（门店经营状态） ====================

    /**
     * 获得门店经营状态
     *
     * 【What】
     * 查询门店的经营状态，包括当前状态、开店日期、签约日期等
     *
     * 【Constraints】
     * - 一对一关系
     * - store_status=0 表示已开店，store_status=1 表示已关店
     *
     * @param storeId 门店ID
     * @return 门店经营状态
     */
    BusinessStatusTableDO getStatusTableByStoreId(String storeId);

    // ==================== 子表（门店加盟商信息） ====================

    /**
     * 获得门店加盟商信息
     *
     * 【What】
     * 查询门店的加盟商信息，包括加盟商名称、联系方式等
     *
     * 【Constraints】
     * - 一对一关系
     *
     * @param storeId 门店ID
     * @return 门店加盟商信息
     */
    FranchiseeTableDO getFranchiseeTableByStoreId(String storeId);

    // ==================== 子表（门店联系人通讯录） ====================

    /**
     * 获得门店联系人通讯录列表
     *
     * 【What】
     * 查询门店的所有联系人信息，包括姓名、电话、角色等
     *
     * 【Constraints】
     * - 一对多关系，一个门店可有多个联系人
     * - contactId 为联系人主键，用于去重和更新判断
     *
     * 【Pitfalls】
     * - 【教训2024-03】ContactTable 使用 diffList 算法，依赖 contactId 判断同一条记录
     * - 如果 Excel 导入时未保留 contactId，会导致每次导入都新增而非更新
     *
     * @param storeId 门店ID
     * @return 门店联系人通讯录列表
     */
    List<ContactTableDO> getContactTableListByStoreId(String storeId);

    // ==================== 子表（门店平台关联） ====================

    /**
     * 获得门店平台关联列表
     *
     * 【What】
     * 查询门店与各平台的关联信息，包括 platformId、platformStoreId 等
     *
     * 【Constraints】
     * - 一对多关系，一个门店可关联多个平台
     * - 返回结果包含平台名称（需要 JOIN platform_table）
     *
     * @param storeId 门店ID
     * @return 门店平台关联列表
     */
    List<cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO> getPlatformTableListByStoreId(String storeId);

    // ==================== 导入功能 ====================

    /**
     * 导入门店列表
     *
     * 【What】
     * 批量导入门店数据，支持新增和更新两种模式：
     * - 新增模式：storeId/storeName 均不存在时创建
     * - 更新模式：storeId/storeName 任一匹配时更新（isUpdateSupport=true）
     *
     * 【Constraints】
     * - 必须使用 @Transactional(rollbackFor = Exception.class)
     * - 导入结果返回成功/失败列表，便于定位问题
     * - 每次导入后同步 Redis 缓存
     *
     * 【Pitfalls】
     * - 【教训2024-03】导入时不校验子表ID，导致子表被覆盖而非更新
     * - forEach 内 try-catch 不会自动回滚事务，部分失败时需手动处理
     * - 导入时未处理 contactId，可能导致联系人重复创建
     *
     * @param importList 导入列表
     * @param isUpdateSupport 是否支持更新
     * @return 导入结果
     */
    StoreImportRespVO importStoreList(List<StoreImportExcelVO> importList, boolean isUpdateSupport);

    /**
     * 获取门店简单信息列表
     *
     * 【What】
     * 返回门店的简化信息（名称、状态、平台关联），用于下拉选择、列表展示等轻量场景
     *
     * 【Constraints】
     * - 可按 platformId 筛选，未传则返回全部
     * - 返回结果已去重，同一平台只保留一条关联记录
     *
     * @param platformId 平台ID（可选）
     * @return 门店简单信息列表
     */
    List<StoreSimpleRespVO> getStoreSimpleList(Long platformId);

    /**
     * 根据平台门店ID获取门店平台关联信息
     *
     * 【What】
     * 通过第三方平台的 storeId 查询本地门店的关联信息
     * 先查 Redis 缓存，未命中则查数据库
     *
     * 【Constraints】
     * - platformStoreId 会进行 trim 处理，支持前后空格
     * - 返回 null 表示未找到匹配记录
     *
     * 【Pitfalls】
     * - 缓存未命中时会打印 ERROR 日志，生产环境需关注
     * - 缓存 key 格式需与 syncStorePlatformInfoToRedis() 保持一致
     *
     * @param platformStoreId 平台门店ID
     * @return 门店平台关联信息
     */
    cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO getPlatformTableByPlatformStoreId(String platformStoreId);

    /**
     * 根据平台和平台门店ID获取全部有效门店平台关联信息
     *
     * @param platformId 平台ID
     * @param platformStoreId 平台门店ID
     * @return 门店平台关联信息列表
     */
    List<cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO> getPlatformTableListByPlatformStoreId(Long platformId, String platformStoreId);

    List<cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO> getOpenPlatformStores(Long platformId);

    /**
     * 获取定时同步使用的平台门店列表
     *
     * 【What】
     * 获取已开店（store_status=0）的门店的平台关联信息
     * 先查 Redis 缓存，未命中则查询数据库并回填缓存
     * platformCode 参数保留，当前实现已不依赖此参数
     *
     * 【Why - 为什么不用 platformCode？】
     * 原实现根据 platformCode 筛选，但业务上需要查询所有平台的关联信息，
     * 改为查询所有已开店的门店后统一处理
     *
     * 【Constraints】
     * - 只返回有 platformStoreId 的门店，无关联的门店会被跳过
     * - 数据库查询使用 store_status=0 条件，过滤已关店门店
     *
     * 【Pitfalls】
     * - 【教训2024-03】缓存与数据库不一致时，同步任务会漏掉门店
     * - 需确保每次门店状态变更后同步 Redis
     *
     * @param platformCode 平台编码（保留参数，当前实现不使用）
     * @return 平台门店列表
     */
    List<cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO> getOpenPlatformStoresByPlatformCode(String platformCode);

    /**
     * 获取全部门店的平台关联列表（缓存优先，含开店和关店）
     *
     * 【What】
     * 先查 Redis 缓存，未命中则查询数据库并回填缓存
     * 返回全部门店（不管开店关店），用于订单同步等需要全量门店的场景
     *
     * 【Constraints】
     * - 只返回有 platformStoreId 的门店，无关联的门店会被跳过
     * - 不过滤 storeStatus，全部门店都返回
     *
     * @param platformCode 平台编码（保留参数，当前实现不使用）
     * @return 全部平台门店列表
     */
    List<StorePlatformRespVO> getAllPlatformStoresByPlatformCode(String platformCode);

    /**
     * 从Redis获取已开店门店的平台信息列表
     *
     * 【What】
     * 获取缓存中的门店平台信息，用于 Ele 订单同步等高频查询场景
     *
     * 【Constraints】
     * - 依赖 syncStorePlatformInfo() 定期刷新缓存
     * - 缓存为空时返回空列表，调用方需处理降级逻辑
     *
     * @return 门店平台信息列表
     */
    List<StorePlatformInfoRespVO> getStorePlatformInfoList();

    /**
     * 手动同步门店平台信息到Redis
     *
     * 【What】
     * 主动刷新 Redis 缓存，确保数据一致性
     * 在门店增删改、导入完成后调用
     *
     * 【Constraints】
     * - 此方法为同步调用，Redis 操作耗时约 10-50ms
     * - 避免在高并发写入场景频繁调用，建议批量操作后统一刷新
     *
     * @return void
     */
    void syncStorePlatformInfo();

}