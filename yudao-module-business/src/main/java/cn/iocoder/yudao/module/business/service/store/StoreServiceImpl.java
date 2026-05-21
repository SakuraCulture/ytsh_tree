package cn.iocoder.yudao.module.business.service.store;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSimpleRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformInfoRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSupplyLineRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineStoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.SpaceTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.AffiliationTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.BusinessStatusTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.FranchiseeTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.ContactTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineStoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStoreSupplyMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.service.store.StorePlatformCacheService;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

/**
 * 门店服务实现类
 * 
 * @author 彼岸花
 */
@Slf4j
@Service
@Validated
public class StoreServiceImpl implements StoreService {

    @Resource
    private StoreMapper storeMapper;
    @Resource
    private SpaceTableMapper spaceTableMapper;
    @Resource
    private AffiliationTableMapper affiliationTableMapper;
    @Resource
    private BusinessStatusTableMapper statusTableMapper;
    @Resource
    private FranchiseeTableMapper franchiseeTableMapper;
    @Resource
    private ContactTableMapper contactTableMapper;
    @Resource
    private PlatformTableMapper platformTableMapper;
    @Resource
    private PlatformMapper platformMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseLineMapper warehouseLineMapper;
    @Resource
    private WarehouseLineStoreMapper warehouseLineStoreMapper;
    @Resource
    private StorePlatformCacheService storePlatformCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createStore(StoreSaveReqVO createReqVO) {
        // 检查门店编码是否已存在
        StoreDO existStore = storeMapper.selectById(createReqVO.getStoreId());
        if (existStore != null) {
            throw exception(STORE_CODE_EXISTS);
        }
        // 检查门店名称是否已存在
        existStore = storeMapper.selectByStoreName(createReqVO.getStoreName());
        if (existStore != null) {
            throw exception(STORE_NAME_EXISTS);
        }
        // 插入
        StoreDO store = BeanUtils.toBean(createReqVO, StoreDO.class);
        storeMapper.insert(store);

        // 插入子表
        createSpaceTable(store.getStoreId(), createReqVO.getSpaceTable());
        createAffiliationTable(store.getStoreId(), createReqVO.getAffiliationTable());
        createStatusTable(store.getStoreId(), createReqVO.getStatusTable());
        createFranchiseeTable(store.getStoreId(), createReqVO.getFranchiseeTable());
        createContactTableList(store.getStoreId(), createReqVO.getContactTables());
        refreshStorePlatformCacheAfterCommit();
        // 返回
        return store.getStoreId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStore(StoreSaveReqVO updateReqVO) {
        // 校验存在
        StoreDO existStore = validateStoreExists(updateReqVO.getStoreId());
        // 检查门店编码是否被其他门店使用
        if (!existStore.getStoreId().equals(updateReqVO.getStoreId())) {
            StoreDO storeByCode = storeMapper.selectById(updateReqVO.getStoreId());
            if (storeByCode != null) {
                throw exception(STORE_CODE_EXISTS);
            }
        }
        // 检查门店名称是否被其他门店使用
        if (!existStore.getStoreName().equals(updateReqVO.getStoreName())) {
            StoreDO storeByName = storeMapper.selectByStoreName(updateReqVO.getStoreName());
            if (storeByName != null) {
                throw exception(STORE_NAME_EXISTS);
            }
        }
        // 更新
        StoreDO updateObj = BeanUtils.toBean(updateReqVO, StoreDO.class);
        storeMapper.updateById(updateObj);

        // 更新子表
        updateSpaceTable(updateReqVO.getStoreId(), updateReqVO.getSpaceTable());
        updateAffiliationTable(updateReqVO.getStoreId(), updateReqVO.getAffiliationTable());
        updateStatusTable(updateReqVO.getStoreId(), updateReqVO.getStatusTable());
        updateFranchiseeTable(updateReqVO.getStoreId(), updateReqVO.getFranchiseeTable());
        updateContactTableList(updateReqVO.getStoreId(), updateReqVO.getContactTables());
        refreshStorePlatformCacheAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(String id) {
        // 校验存在
        validateStoreExists(id);
        // 删除
        storeMapper.deleteById(id);

        // 删除子表
        deleteSpaceTableByStoreId(id);
        deleteAffiliationTableByStoreId(id);
        deleteStatusTableByStoreId(id);
        deleteFranchiseeTableByStoreId(id);
        deleteContactTableByStoreId(id);
        refreshStorePlatformCacheAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStoreListByIds(List<String> ids) {
        // 删除
        storeMapper.deleteByIds(ids);

        // 删除子表
        deleteSpaceTableByStoreIds(ids);
        deleteAffiliationTableByStoreIds(ids);
        deleteStatusTableByStoreIds(ids);
        deleteFranchiseeTableByStoreIds(ids);
        deleteContactTableByStoreIds(ids);
        refreshStorePlatformCacheAfterCommit();
    }

    private void refreshStorePlatformCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storePlatformCacheService.syncStorePlatformInfoToRedis();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storePlatformCacheService.syncStorePlatformInfoToRedis();
            }
        });
    }

    private StoreDO validateStoreExists(String id) {
        StoreDO store = storeMapper.selectById(id);
        if (store == null) {
            throw exception(STORE_NOT_EXISTS);
        }
        return store;
    }

    @Override
    public StoreDO getStore(String id) {
        return storeMapper.selectById(id);
    }

    @Override
    public PageResult<StoreDO> getStorePage(StorePageReqVO pageReqVO) {
        return storeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<StoreImportExcelVO> getStoreImportExcelList(StorePageReqVO pageReqVO) {
        StorePageReqVO exportReqVO = BeanUtils.toBean(pageReqVO, StorePageReqVO.class);
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StoreDO> stores = getStorePage(exportReqVO).getList();
        if (CollUtil.isEmpty(stores)) {
            return Collections.emptyList();
        }
        return stores.stream().map(store -> {
            StoreImportExcelVO row = BeanUtils.toBean(store, StoreImportExcelVO.class);
            SpaceTableDO space = spaceTableMapper.selectByStoreId(store.getStoreId());
            if (space != null) {
                row.setBuildingArea(space.getBuildingArea());
                row.setColdStorageArea(space.getColdStorageArea());
            }
            AffiliationTableDO affiliation = affiliationTableMapper.selectByStoreId(store.getStoreId());
            if (affiliation != null) {
                row.setBusinessMode(affiliation.getBusinessMode());
                row.setStoreType(affiliation.getStoreType());
            }
            BusinessStatusTableDO status = statusTableMapper.selectByStoreId(store.getStoreId());
            if (status != null) {
                row.setCurrentStatus(status.getCurrentStatus());
                row.setOpenDate(status.getOpenDate());
                row.setSignDate(status.getSignDate());
            }
            FranchiseeTableDO franchisee = franchiseeTableMapper.selectByStoreId(store.getStoreId());
            if (franchisee != null) {
                row.setFranchiseeName(franchisee.getFranchiseeName());
                row.setFranchiseePhone(franchisee.getFranchiseePhone());
                row.setFranchiseeFee(franchisee.getFranchiseeFee());
                row.setSecurityDeposit(franchisee.getSecurityDeposit());
                row.setContractStart(franchisee.getContractStart());
                row.setContractEnd(franchisee.getContractEnd());
            }
            return row;
        }).collect(Collectors.toList());
    }

    // ==================== 子表（门店空间） ====================

    /**
     * 【What】查询门店空间信息
     * 【Constraints】一对一关系，返回 null 表示未维护
     */
    @Override
    public SpaceTableDO getSpaceTableByStoreId(String storeId) {
        return spaceTableMapper.selectByStoreId(storeId);
    }

    /**
     * 【What】创建门店空间
     * 【Why】空间信息非必填，null 时跳过创建
     */
    private void createSpaceTable(String storeId, SpaceTableDO spaceTable) {
        if (spaceTable == null) {
            return;
        }
        spaceTable.setStoreId(storeId);
        spaceTableMapper.insert(spaceTable);
    }

    /**
     * 【What】更新门店空间
     * 【Why - .clean() 的作用】
     * edit 页面回显时，BeanUtils.toBean() 复制了完整的 SpaceTableDO，包括 createTime/updateTime
     * 调用 insertOrUpdate() 时，如果 spaceSpaceId 有值，MP 执行 UPDATE
     * 此时 updateTime 字段使用数据库默认值，导致审计时间不更新
     * 解决：在 setStoreId() 后调用 .clean()，清除审计字段，让 MP 自动填充当前时间
     */
    private void updateSpaceTable(String storeId, SpaceTableDO spaceTable) {
        if (spaceTable == null) {
            deleteSpaceTableByStoreId(storeId);
            return;
        }
        // 【Why - 必须清除审计字段，否则 updateTime 不更新】
        spaceTable.setStoreId(storeId).clean();
        spaceTableMapper.insertOrUpdate(spaceTable);
    }

    /**
     * 【What】删除门店空间（单条）
     */
    private void deleteSpaceTableByStoreId(String storeId) {
        spaceTableMapper.deleteByStoreId(storeId);
    }

    /**
     * 【What】删除门店空间（批量）
     * 【Constraints】使用 IN 条件删除
     */
    private void deleteSpaceTableByStoreIds(List<String> storeIds) {
        spaceTableMapper.deleteByStoreIds(storeIds);
    }

    // ==================== 子表（门店架构归属） ====================

    /**
     * 【What】查询门店架构归属
     * 【Constraints】一对一关系
     */
    @Override
    public AffiliationTableDO getAffiliationTableByStoreId(String storeId) {
        return affiliationTableMapper.selectByStoreId(storeId);
    }

    /**
     * 【What】创建门店架构归属
     * 【Why】AffiliationTable 非必填
     */
    private void createAffiliationTable(String storeId, AffiliationTableDO affiliationTable) {
        if (affiliationTable == null) {
            return;
        }
        affiliationTable.setStoreId(storeId);
        affiliationTableMapper.insert(affiliationTable);
    }

    /**
     * 【What】更新门店架构归属
     * 【Why - .clean() 必要性同上】
     */
    private void updateAffiliationTable(String storeId, AffiliationTableDO affiliationTable) {
        if (affiliationTable == null) {
            deleteAffiliationTableByStoreId(storeId);
            return;
        }
        // 【Why - 必须清除审计字段，否则 updateTime 不更新】
        affiliationTable.setStoreId(storeId).clean();
        affiliationTableMapper.insertOrUpdate(affiliationTable);
    }

    /**
     * 【What】删除门店架构归属（单条）
     */
    private void deleteAffiliationTableByStoreId(String storeId) {
        affiliationTableMapper.deleteByStoreId(storeId);
    }

    /**
     * 【What】删除门店架构归属（批量）
     */
    private void deleteAffiliationTableByStoreIds(List<String> storeIds) {
        affiliationTableMapper.deleteByStoreIds(storeIds);
    }

    // ==================== 子表（门店经营状态） ====================

    /**
     * 【What】查询门店经营状态
     * 【Constraints】一对一关系，store_status=0 已开店，store_status=1 已关店
     */
    @Override
    public BusinessStatusTableDO getStatusTableByStoreId(String storeId) {
        return statusTableMapper.selectByStoreId(storeId);
    }

    /**
     * 【What】创建门店经营状态
     * 【Why】StatusTable 非必填，但业务上一般都会维护
     */
    private void createStatusTable(String storeId, BusinessStatusTableDO statusTable) {
        if (statusTable == null) {
            return;
        }
        statusTable.setStoreId(storeId);
        statusTableMapper.insert(statusTable);
    }

    /**
     * 【What】更新门店经营状态
     * 【Why - .clean() 必要性同上】
     */
    private void updateStatusTable(String storeId, BusinessStatusTableDO statusTable) {
        if (statusTable == null) {
            deleteStatusTableByStoreId(storeId);
            return;
        }
        // 【Why - 必须清除审计字段，否则 updateTime 不更新】
        statusTable.setStoreId(storeId).clean();
        statusTableMapper.insertOrUpdate(statusTable);
    }

    /**
     * 【What】删除门店经营状态（单条）
     */
    private void deleteStatusTableByStoreId(String storeId) {
        statusTableMapper.deleteByStoreId(storeId);
    }

    /**
     * 【What】删除门店经营状态（批量）
     */
    private void deleteStatusTableByStoreIds(List<String> storeIds) {
        statusTableMapper.deleteByStoreIds(storeIds);
    }

    // ==================== 子表（门店加盟商信息） ====================

    /**
     * 【What】查询门店加盟商信息
     * 【Constraints】一对一关系
     */
    @Override
    public FranchiseeTableDO getFranchiseeTableByStoreId(String storeId) {
        return franchiseeTableMapper.selectByStoreId(storeId);
    }

    /**
     * 【What】创建门店加盟商信息
     * 【Why】FranchiseeTable 非必填
     */
    private void createFranchiseeTable(String storeId, FranchiseeTableDO franchiseeTable) {
        if (franchiseeTable == null) {
            return;
        }
        franchiseeTable.setStoreId(storeId);
        franchiseeTableMapper.insert(franchiseeTable);
    }

    /**
     * 【What】更新门店加盟商信息
     * 【Why - .clean() 必要性同上】
     */
    private void updateFranchiseeTable(String storeId, FranchiseeTableDO franchiseeTable) {
        if (franchiseeTable == null) {
            deleteFranchiseeTableByStoreId(storeId);
            return;
        }
        // 【Why - 必须清除审计字段，否则 updateTime 不更新】
        franchiseeTable.setStoreId(storeId).clean();
        franchiseeTableMapper.insertOrUpdate(franchiseeTable);
    }

    /**
     * 【What】删除门店加盟商信息（单条）
     */
    private void deleteFranchiseeTableByStoreId(String storeId) {
        franchiseeTableMapper.deleteByStoreId(storeId);
    }

    /**
     * 【What】删除门店加盟商信息（批量）
     */
    private void deleteFranchiseeTableByStoreIds(List<String> storeIds) {
        franchiseeTableMapper.deleteByStoreIds(storeIds);
    }

    // ==================== 子表（门店联系人通讯录） ====================

    /**
     * 【What】查询门店联系人列表
     * 【Constraints】一对多关系，返回 List，可能为空
     */
    @Override
    public List<ContactTableDO> getContactTableListByStoreId(String storeId) {
        return contactTableMapper.selectListByStoreId(storeId);
    }

    /**
     * 【What】批量创建联系人
     * 【Why - .clean() 必要性】同上，清除审计字段
     */
    private void createContactTableList(String storeId, List<ContactTableDO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(o -> o.setStoreId(storeId).clean());
        contactTableMapper.insertBatch(list);
    }

    /**
     * 【What】增量更新联系人列表
     *
     * 【Why - 为什么用 diffList？】
     * 一对多关系，如果用全量删除再插入：
     * deleteByStoreId(storeId);
     * insertBatch(newList);
     * 问题：contactId 每次都变，关联的业务数据（订单、日志）外键失效
     *
     * diffList 算法：
     * 1. 查询 oldList（数据库中原有记录）
     * 2. 与 newList（新传入记录）做 diff
     * 3. 分三类处理：
     * - diffList.get(0)：需新增（oldList 有，newList 无对应ID）→ insertBatch
     * - diffList.get(1)：需更新（oldList、newList 都有同一 contactId）→ updateBatch
     * - diffList.get(2)：需删除（oldList 有，newList 无）→ deleteBatch
     *
     * 【Constraints】
     * - 依赖 contactId 做匹配，contactId 为空则视为新增
     * - 匹配条件：oldVal.getContactId() == newVal.getContactId()
     *
     * 【Pitfalls】
     * - 【教训2024-03】匹配条件有误：当前用 == 比较 Long 类型，可能导致自动装箱比较失效
     * 建议改为 ObjectUtil.equal(oldVal.getContactId(), newVal.getContactId())
     */
    private void updateContactTableList(String storeId, List<ContactTableDO> list) {
        if (list == null) {
            return;
        }
        // 【Why - 必须清除审计字段，否则 updateTime 不更新】
        list.forEach(o -> o.setStoreId(storeId).clean());
        List<ContactTableDO> oldList = contactTableMapper.selectListByStoreId(storeId);
        List<List<ContactTableDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            // 【Pitfalls - Long 类型比较需用 ObjectUtil.equal】
            boolean same = ObjectUtil.equal(oldVal.getContactId(), newVal.getContactId());
            if (same) {
                // 匹配成功，保留原 ID 并清除审计字段
                newVal.setContactId(oldVal.getContactId()).clean();
            }
            return same;
        });

        // 【第二步：批量添加、修改、删除】
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            contactTableMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            contactTableMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            contactTableMapper.deleteByIds(convertList(diffList.get(2), ContactTableDO::getContactId));
        }
    }

    /**
     * 【What】删除联系人（单条）
     */
    private void deleteContactTableByStoreId(String storeId) {
        contactTableMapper.deleteByStoreId(storeId);
    }

    /**
     * 【What】删除联系人（批量）
     */
    private void deleteContactTableByStoreIds(List<String> storeIds) {
        contactTableMapper.deleteByStoreIds(storeIds);
    }

    // ==================== 子表（门店平台关联） ====================

    /**
     * 【What】查询门店的平台关联列表
     *
     * 【处理流程】
     * 1. 查询 platform_table（按 storeId）
     * 2. 提取所有 platformId
     * 3. 批量查询 platform_table 获取平台名称
     * 4. 组装返回 VO（包含平台名称）
     *
     * 【Why - 为什么要两次查询？】
     * platform_table 和 platform_table 是两张表，需要 JOIN
     * 直接 JOIN 可能导致 N+1 问题，这里优化为 2 次查询
     *
     * 【Constraints】
     * - 返回空列表表示无关联（非 null）
     */
    @Override
    public List<StorePlatformRespVO> getPlatformTableListByStoreId(String storeId) {
        List<PlatformTableDO> list = platformTableMapper.selectListByStoreId(storeId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 获取所有平台ID
        Set<Long> platformIds = new HashSet<>();
        for (PlatformTableDO item : list) {
            if (item.getPlatformId() != null) {
                platformIds.add(item.getPlatformId());
            }
        }
        // 查询平台信息
        Map<Long, PlatformDO> platformMap = new HashMap<>();
        if (CollUtil.isNotEmpty(platformIds)) {
            List<PlatformDO> platforms = platformMapper.selectList(new LambdaQueryWrapperX<PlatformDO>()
                    .in(PlatformDO::getPlatformId, platformIds));
            for (PlatformDO platform : platforms) {
                platformMap.put(platform.getPlatformId(), platform);
            }
        }
        // 组装返回数据
        List<StorePlatformRespVO> result = new ArrayList<>();
        for (PlatformTableDO item : list) {
            StorePlatformRespVO vo = BeanUtils.toBean(item, StorePlatformRespVO.class);
            if (item.getPlatformId() != null && platformMap.containsKey(item.getPlatformId())) {
                vo.setPlatformName(platformMap.get(item.getPlatformId()).getPlatformName());
            }
            result.add(vo);
        }
        return result;
    }

    // ==================== 导入功能 ====================

    /**
     * 【What】批量导入门店数据
     *
     * 【导入流程 - 三步走】
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 第一步：数据准备 │
     * │ - 校验导入列表非空 │
     * │ - 构建响应 VO（createNames、updateNames、failureStoreNames）│
     * └─────────────────────────────────────────────────────────────┘
     * ↓
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 第二步：逐条处理（forEach + try-catch） │
     * │ │
     * │ 判断逻辑： │
     * │ ┌──────────────────────────────────────────────────────┐ │
     * │ │ storeId 存在？ → selectById(storeId) │ │
     * │ │ storeName 存在？ → selectByStoreName(storeName) │ │
     * │ │ 都不存在 → 新增模式 │ │
     * │ │ 存在 + isUpdateSupport=true → 更新模式 │ │
     * │ │ 存在 + isUpdateSupport=false → 失败 │ │
     * │ └──────────────────────────────────────────────────────┘ │
     * │ │
     * │ 新增模式处理： │
     * │ - insert 主表 │
     * │ - 按条件插入子表（SpaceTable、AffiliationTable...） │
     * │ │
     * │ 更新模式处理： │
     * │ - update 主表 │
     * │ - 子表需先查再决定 insert/update（避免覆盖） │
     * └─────────────────────────────────────────────────────────────┘
     * ↓
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 第三步：请求刷新缓存 & 返回 │
     * │ - refreshStorePlatformCacheAfterCommit() │
     * │ - 返回导入结果 │
     * └─────────────────────────────────────────────────────────────┘
     *
     * 【Why - 为什么用 forEach + try-catch？】
     * 批量导入时，部分数据失败不应影响其他数据正常入库
     * 但 forEach 内异常不会自动回滚事务，需注意：
     * - 异常被 catch 吞掉，记录到 failureStoreNames
     * - 未被 catch 的异常会触发事务回滚
     *
     * 【Constraints】
     * - 必须 @Transactional(rollbackFor = Exception.class)
     * - 导入列表不宜过大（建议 < 1000 条）
     * - 子表插入前需检查字段是否为空（避免全量覆盖）
     *
     * 【Pitfalls】
     * - 【教训2024-03】更新模式子表处理有误：
     * 当前实现先查 existXxx，再决定 insert/update
     * 但 insertOrUpdate 时未传子表 ID，会变成覆盖而非更新
     * - 【教训2024-03】ContactTable 导入问题：
     * 导入时不保留 contactId，每次都新增，导致联系人重复
     * - 【教训2024-03】批量导入性能差：
     * forEach 单条处理，1000+ 条时耗时较长
     *
     * 【未来优化方向】
     * - 改用批量 SQL 插入（Batch Insert）
     * - 子表更新时需传入原 ID，改为 updateById 而非 insertOrUpdate
     * - ContactTable 导入需支持按姓名+电话去重
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreImportRespVO importStoreList(List<StoreImportExcelVO> importList, boolean isUpdateSupport) {
        // 【校验】导入列表不能为空
        if (CollUtil.isEmpty(importList)) {
            throw exception(STORE_IMPORT_LIST_IS_EMPTY);
        }

        // 【初始化】构建响应对象
        StoreImportRespVO respVO = StoreImportRespVO.builder()
                .createStoreNames(new ArrayList<>())
                .updateStoreNames(new ArrayList<>())
                .failureStoreNames(new LinkedHashMap<>())
                .build();

        // 【遍历处理】逐条导入，使用 try-catch 避免单条失败影响整体
        importList.forEach(importStore -> {
            try {
                // 【查询逻辑】先按 storeId 查，再按 storeName 查
                StoreDO existStore = null;
                if (StrUtil.isNotBlank(importStore.getStoreId())) {
                    existStore = storeMapper.selectById(importStore.getStoreId());
                }
                if (existStore == null) {
                    existStore = storeMapper.selectByStoreName(importStore.getStoreName());
                }

                // 【分支1：新增模式】storeId 和 storeName 都不存在
                if (existStore == null) {
                    // 插入主表
                    StoreDO store = BeanUtils.toBean(importStore, StoreDO.class);
                    storeMapper.insert(store);

                    // 【按条件插入子表】避免覆盖已有数据
                    // 条件判断：只要有一个非空字段就插入
                    if (importStore.getBuildingArea() != null || importStore.getColdStorageArea() != null) {
                        SpaceTableDO space = BeanUtils.toBean(importStore, SpaceTableDO.class);
                        space.setStoreId(store.getStoreId());
                        spaceTableMapper.insert(space);
                    }

                    if (importStore.getBusinessMode() != null || importStore.getStoreType() != null) {
                        AffiliationTableDO affiliation = BeanUtils.toBean(importStore, AffiliationTableDO.class);
                        affiliation.setStoreId(store.getStoreId());
                        affiliationTableMapper.insert(affiliation);
                    }

                    if (importStore.getCurrentStatus() != null || importStore.getOpenDate() != null
                            || importStore.getSignDate() != null) {
                        BusinessStatusTableDO status = BeanUtils.toBean(importStore, BusinessStatusTableDO.class);
                        status.setStoreId(store.getStoreId());
                        statusTableMapper.insert(status);
                    }

                    if (hasFranchiseeImportValue(importStore)) {
                        FranchiseeTableDO franchisee = BeanUtils.toBean(importStore, FranchiseeTableDO.class);
                        franchisee.setStoreId(store.getStoreId());
                        franchiseeTableMapper.insert(franchisee);
                    }

                    // 记录成功创建
                    respVO.getCreateStoreNames().add(importStore.getStoreName());

                    // 【分支2：更新模式】门店存在 + isUpdateSupport=true
                } else if (isUpdateSupport) {
                    // 更新主表
                    StoreDO updateStore = BeanUtils.toBean(importStore, StoreDO.class);
                    updateStore.setStoreId(existStore.getStoreId());
                    storeMapper.updateById(updateStore);

                    // 【关键点】子表更新前先查原记录，避免 insertOrUpdate 变成覆盖
                    if (importStore.getBuildingArea() != null || importStore.getColdStorageArea() != null) {
                        SpaceTableDO existSpace = spaceTableMapper.selectByStoreId(existStore.getStoreId());
                        SpaceTableDO space = BeanUtils.toBean(importStore, SpaceTableDO.class);
                        if (existSpace != null) {
                            // 【Pitfalls】必须传原 ID，否则会变成覆盖
                            space.setStoreSpaceId(existSpace.getStoreSpaceId());
                            spaceTableMapper.updateById(space);
                        } else {
                            space.setStoreId(existStore.getStoreId());
                            spaceTableMapper.insert(space);
                        }
                    }

                    if (importStore.getBusinessMode() != null || importStore.getStoreType() != null) {
                        AffiliationTableDO existAffiliation = affiliationTableMapper
                                .selectByStoreId(existStore.getStoreId());
                        AffiliationTableDO affiliation = BeanUtils.toBean(importStore, AffiliationTableDO.class);
                        if (existAffiliation != null) {
                            affiliation.setAffiliationId(existAffiliation.getAffiliationId());
                            affiliationTableMapper.updateById(affiliation);
                        } else {
                            affiliation.setStoreId(existStore.getStoreId());
                            affiliationTableMapper.insert(affiliation);
                        }
                    }

                    if (importStore.getCurrentStatus() != null || importStore.getOpenDate() != null
                            || importStore.getSignDate() != null) {
                        BusinessStatusTableDO existStatus = statusTableMapper.selectByStoreId(existStore.getStoreId());
                        BusinessStatusTableDO status = BeanUtils.toBean(importStore, BusinessStatusTableDO.class);
                        if (existStatus != null) {
                            status.setStoreBusinessStatusId(existStatus.getStoreBusinessStatusId());
                            statusTableMapper.updateById(status);
                        } else {
                            status.setStoreId(existStore.getStoreId());
                            statusTableMapper.insert(status);
                        }
                    }

                    if (hasFranchiseeImportValue(importStore)) {
                        FranchiseeTableDO existFranchisee = franchiseeTableMapper
                                .selectByStoreId(existStore.getStoreId());
                        FranchiseeTableDO franchisee = BeanUtils.toBean(importStore, FranchiseeTableDO.class);
                        if (existFranchisee != null) {
                            franchisee.setFranchiseeId(existFranchisee.getFranchiseeId());
                            franchiseeTableMapper.updateById(franchisee);
                        } else {
                            franchisee.setStoreId(existStore.getStoreId());
                            franchiseeTableMapper.insert(franchisee);
                        }
                    }

                    // 记录成功更新
                    respVO.getUpdateStoreNames().add(importStore.getStoreName());

                    // 【分支3：拒绝模式】门店存在 + isUpdateSupport=false
                } else {
                    respVO.getFailureStoreNames().put(importStore.getStoreName(), "门店已存在，不允许重复导入");
                }

                // 【异常处理】吞掉异常，记录到失败列表，不影响其他数据
            } catch (Exception ex) {
                String key = (importStore.getStoreName() != null && !importStore.getStoreName().isEmpty())
                        ? importStore.getStoreName()
                        : "第 " + (importList.indexOf(importStore) + 1) + " 行";
                respVO.getFailureStoreNames().put(key, ex.getMessage());
            }
        });

        refreshStorePlatformCacheAfterCommit();
        return respVO;
    }

    private boolean hasFranchiseeImportValue(StoreImportExcelVO importStore) {
        return importStore.getFranchiseeName() != null || importStore.getFranchiseePhone() != null
                || importStore.getFranchiseeFee() != null || importStore.getSecurityDeposit() != null
                || importStore.getContractStart() != null || importStore.getContractEnd() != null;
    }

    @Override
    public List<StoreSimpleRespVO> searchPlatformStoreSimpleList(Long platformId, String keyword, Integer pageNo, Integer pageSize) {
        if (platformId == null) {
            return Collections.emptyList();
        }

        String normalizedKeyword = StrUtil.trim(keyword);
        int normalizedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        Set<String> matchedStoreIds = findMatchedStoreIds(normalizedKeyword);

        LambdaQueryWrapperX<PlatformTableDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(PlatformTableDO::getPlatformId, platformId)
                .eq(PlatformTableDO::getStatus, 1)
                .isNotNull(PlatformTableDO::getPlatformStoreId);
        wrapper.orderByAsc(PlatformTableDO::getStoreId);
        if (StrUtil.isNotBlank(normalizedKeyword)) {
            if (CollUtil.isNotEmpty(matchedStoreIds)) {
                wrapper.and(query -> query.like(PlatformTableDO::getPlatformStoreId, normalizedKeyword)
                        .or()
                        .in(PlatformTableDO::getStoreId, matchedStoreIds));
            } else {
                wrapper.like(PlatformTableDO::getPlatformStoreId, normalizedKeyword);
            }
        }

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(normalizedPageNo);
        pageParam.setPageSize(normalizedPageSize);
        PageResult<PlatformTableDO> pageResult = platformTableMapper.selectPage(pageParam, wrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return Collections.emptyList();
        }

        Map<String, StoreDO> storeMap = getStoreMapByIds(pageResult.getList().stream()
                .map(PlatformTableDO::getStoreId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<StoreSimpleRespVO> result = new ArrayList<>();
        for (PlatformTableDO platformTable : pageResult.getList()) {
            StoreDO store = storeMap.get(platformTable.getStoreId());
            if (store == null) {
                continue;
            }
            result.add(buildStoreSimpleRespVO(store, platformTable));
        }
        return result;
    }

    @Override
    public List<StoreSimpleRespVO> getPlatformStoreSimpleList(Long platformId, List<String> platformStoreIds) {
        if (platformId == null || CollUtil.isEmpty(platformStoreIds)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> normalizedIds = platformStoreIds.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }

        Map<String, StoreSimpleRespVO> resultMap = new LinkedHashMap<>();
        List<StorePlatformInfoRespVO> cachedList = storePlatformCacheService.getStorePlatformListFromRedis();
        if (CollUtil.isNotEmpty(cachedList)) {
            for (StorePlatformInfoRespVO cached : cachedList) {
                if (!Objects.equals(platformId, cached.getPlatformId())) {
                    continue;
                }
                String normalizedPlatformStoreId = StrUtil.trim(cached.getPlatformStoreId());
                if (!normalizedIds.contains(normalizedPlatformStoreId) || resultMap.containsKey(normalizedPlatformStoreId)) {
                    continue;
                }
                StoreSimpleRespVO vo = new StoreSimpleRespVO();
                vo.setStoreId(cached.getStoreId());
                vo.setStoreName(cached.getStoreName());
                vo.setPlatformId(cached.getPlatformId());
                vo.setPlatformStoreId(normalizedPlatformStoreId);
                vo.setStoreStatus(cached.getStoreStatus());
                resultMap.put(normalizedPlatformStoreId, vo);
            }
        }

        if (resultMap.size() < normalizedIds.size()) {
            LinkedHashSet<String> missingIds = normalizedIds.stream()
                    .filter(item -> !resultMap.containsKey(item))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (CollUtil.isNotEmpty(missingIds)) {
                List<PlatformTableDO> platformTables = platformTableMapper.selectEnabledListByPlatformIdAndPlatformStoreIds(platformId, missingIds);
                Map<String, StoreDO> storeMap = getStoreMapByIds(platformTables.stream()
                        .map(PlatformTableDO::getStoreId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
                for (PlatformTableDO platformTable : platformTables) {
                    StoreDO store = storeMap.get(platformTable.getStoreId());
                    if (store == null) {
                        continue;
                    }
                    StoreSimpleRespVO vo = buildStoreSimpleRespVO(store, platformTable);
                    resultMap.putIfAbsent(StrUtil.trim(vo.getPlatformStoreId()), vo);
                }
            }
        }

        List<StoreSimpleRespVO> result = new ArrayList<>();
        for (String normalizedId : normalizedIds) {
            StoreSimpleRespVO vo = resultMap.get(normalizedId);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<StoreSimpleRespVO> getAllSimpleList(Long platformId) {
        List<StorePlatformInfoRespVO> cachedList = storePlatformCacheService.getStorePlatformListFromRedis();
        if (CollUtil.isEmpty(cachedList)) {
            return Collections.emptyList();
        }

        List<StoreSimpleRespVO> result = new ArrayList<>();
        for (StorePlatformInfoRespVO cached : cachedList) {
            if (platformId != null && !Objects.equals(platformId, cached.getPlatformId())) {
                continue;
            }
            String normalizedPlatformStoreId = StrUtil.trim(cached.getPlatformStoreId());
            if (StrUtil.isBlank(normalizedPlatformStoreId)) {
                continue;
            }
            StoreSimpleRespVO vo = new StoreSimpleRespVO();
            vo.setStoreId(cached.getStoreId());
            vo.setStoreName(cached.getStoreName());
            vo.setPlatformId(cached.getPlatformId());
            vo.setPlatformStoreId(normalizedPlatformStoreId);
            vo.setStoreStatus(cached.getStoreStatus());
            result.add(vo);
        }
        return result;
    }

    private Set<String> findMatchedStoreIds(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptySet();
        }
        return storeMapper.selectList(new LambdaQueryWrapperX<StoreDO>()
                        .and(query -> query.like(StoreDO::getStoreId, keyword)
                                .or()
                                .like(StoreDO::getStoreName, keyword))
                        .orderByDesc(StoreDO::getStoreId))
                .stream()
                .map(StoreDO::getStoreId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, StoreDO> getStoreMapByIds(Collection<String> storeIds) {
        if (CollUtil.isEmpty(storeIds)) {
            return Collections.emptyMap();
        }
        return storeMapper.selectList(new LambdaQueryWrapperX<StoreDO>()
                        .inIfPresent(StoreDO::getStoreId, storeIds))
                .stream()
                .collect(Collectors.toMap(StoreDO::getStoreId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private StoreSimpleRespVO buildStoreSimpleRespVO(StoreDO store, PlatformTableDO platformTable) {
        StoreSimpleRespVO vo = new StoreSimpleRespVO();
        vo.setStoreId(store.getStoreId());
        vo.setStoreName(store.getStoreName());
        vo.setStoreStatus(store.getStoreStatus());
        vo.setPlatformId(platformTable.getPlatformId());
        vo.setPlatformStoreId(StrUtil.trim(platformTable.getPlatformStoreId()));
        return vo;
    }

    @Override
    public StoreSupplyLineRespVO getStoreSupplyLineSummary(String storeId) {
        StoreDO store = storeMapper.selectById(storeId);
        if (store == null) {
            return null;
        }
        StoreSupplyLineRespVO respVO = new StoreSupplyLineRespVO();
        respVO.setStoreId(store.getStoreId());
        respVO.setStoreName(store.getStoreName());

        List<WarehouseStoreSupplyDO> supplies = warehouseStoreSupplyMapper.selectListByStoreId(storeId);
        Set<String> warehouseIds = new LinkedHashSet<>(supplies.stream()
                .map(WarehouseStoreSupplyDO::getWarehouseId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet()));
        List<WarehouseLineStoreDO> bindings = warehouseLineStoreMapper.selectList(
                new LambdaQueryWrapperX<WarehouseLineStoreDO>()
                        .eq(WarehouseLineStoreDO::getStoreId, storeId)
                        .orderByAsc(WarehouseLineStoreDO::getSortNo)
                        .orderByAsc(WarehouseLineStoreDO::getId));
        Map<Long, WarehouseLineDO> lineMap = warehouseLineMapper.selectListByLineIds(
                bindings.stream().map(WarehouseLineStoreDO::getLineId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(WarehouseLineDO::getLineId, java.util.function.Function.identity(), (a, b) -> a));
        lineMap.values().stream().map(WarehouseLineDO::getWarehouseId).filter(StrUtil::isNotBlank).forEach(warehouseIds::add);
        Map<String, WarehouseDO> warehouseMap = warehouseMapper.selectListByWarehouseIds(warehouseIds)
                .stream().collect(Collectors.toMap(WarehouseDO::getWarehouseId, java.util.function.Function.identity(), (a, b) -> a));

        respVO.setSupplies(supplies.stream().map(item -> {
            StoreSupplyLineRespVO.SupplyItem supply = new StoreSupplyLineRespVO.SupplyItem();
            supply.setWarehouseId(item.getWarehouseId());
            supply.setIsPrimary(item.getIsPrimary());
            supply.setSupplyStatus(item.getSupplyStatus());
            supply.setRemark(item.getRemark());
            WarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                supply.setWarehouseName(warehouse.getWarehouseName());
            }
            if (Integer.valueOf(1).equals(item.getIsPrimary())) {
                respVO.setPrimaryWarehouseId(item.getWarehouseId());
                respVO.setPrimaryWarehouseName(supply.getWarehouseName());
            }
            return supply;
        }).collect(Collectors.toList()));

        respVO.setLines(bindings.stream().map(item -> {
            WarehouseLineDO line = lineMap.get(item.getLineId());
            StoreSupplyLineRespVO.LineItem lineItem = new StoreSupplyLineRespVO.LineItem();
            lineItem.setLineId(item.getLineId());
            lineItem.setSortNo(item.getSortNo());
            if (line != null) {
                lineItem.setWarehouseId(line.getWarehouseId());
                lineItem.setLineCode(line.getLineCode());
                lineItem.setLineName(line.getLineName());
                lineItem.setOrderWeekdays(line.getOrderWeekdays());
                lineItem.setLineStatus(line.getLineStatus());
                WarehouseDO warehouse = warehouseMap.get(line.getWarehouseId());
                if (warehouse != null) {
                    lineItem.setWarehouseName(warehouse.getWarehouseName());
                }
            }
            return lineItem;
        }).collect(Collectors.toList()));

        return respVO;
    }

    /**
     * 【What】根据平台门店ID查询本地关联信息（缓存优先）
     *
     * 【查询流程】
     * 1. trim 处理入参
     * 2. 查 Redis 缓存（快速路径）
     * 3. 缓存未命中则查数据库（兜底）
     *
     * 【Why - 为什么 trim？】
     * Excel 导入或第三方推送可能带前后空格，直接比较会失败
     *
     * 【Pitfalls】
     * - 【教训2024-03】缓存未命中打印 ERROR 日志，生产环境需调整为 INFO 或 WARN
     * - 【教训2024-03】缓存 key 格式需与 syncStorePlatformInfoToRedis() 保持一致
     */
    @Override
    public StorePlatformRespVO getPlatformTableByPlatformStoreId(String platformStoreId) {
        String normalizedPlatformStoreId = StrUtil.trim(platformStoreId);
        if (StrUtil.isBlank(normalizedPlatformStoreId)) {
            return null;
        }

        List<StorePlatformInfoRespVO> cachedList = storePlatformCacheService.getStorePlatformListFromRedis();
        if (CollUtil.isNotEmpty(cachedList)) {
            StorePlatformInfoRespVO cached = cachedList.stream()
                    .filter(item -> StrUtil.equals(normalizedPlatformStoreId, StrUtil.trim(item.getPlatformStoreId())))
                    .findFirst()
                    .orElse(null);
            if (cached != null) {
                StorePlatformRespVO vo = new StorePlatformRespVO();
                vo.setPlatformStoreId(StrUtil.trim(cached.getPlatformStoreId()));
                vo.setPlatformStoreName(cached.getStoreName());
                return vo;
            }
        }

        log.warn("【门店平台关联查询】Redis缓存未命中，查询数据库, platformStoreId={}", normalizedPlatformStoreId);
        return queryDatabaseForPlatformStoreId(normalizedPlatformStoreId);
    }

    @Override
    public List<StorePlatformRespVO> getPlatformTableListByPlatformStoreId(Long platformId, String platformStoreId) {
        String normalizedPlatformStoreId = StrUtil.trim(platformStoreId);
        if (platformId == null || StrUtil.isBlank(normalizedPlatformStoreId)) {
            return Collections.emptyList();
        }
        List<PlatformTableDO> list = platformTableMapper.selectListByPlatformIdAndPlatformStoreId(platformId, normalizedPlatformStoreId);
        return convertPlatformTables(list);
    }

    @Override
    public List<StorePlatformRespVO> getOpenPlatformStores(Long platformId) {
        if (platformId == null) {
            return Collections.emptyList();
        }
        List<StoreDO> stores = storeMapper.selectList(new LambdaQueryWrapperX<StoreDO>()
                .eq(StoreDO::getStoreStatus, 1)
                .orderByAsc(StoreDO::getStoreId));
        if (CollUtil.isEmpty(stores)) {
            return Collections.emptyList();
        }
        Map<String, StoreDO> storeMap = stores.stream()
                .filter(store -> StrUtil.isNotBlank(store.getStoreId()))
                .collect(Collectors.toMap(StoreDO::getStoreId, store -> store, (left, right) -> left, LinkedHashMap::new));
        if (CollUtil.isEmpty(storeMap)) {
            return Collections.emptyList();
        }
        List<PlatformTableDO> platformTables = platformTableMapper.selectEnabledListByPlatformIdAndStoreIds(platformId,
                new ArrayList<>(storeMap.keySet()));
        List<StorePlatformRespVO> result = convertPlatformTables(platformTables);
        for (StorePlatformRespVO item : result) {
            StoreDO store = storeMap.get(item.getStoreId());
            if (store != null) {
                item.setPlatformStoreName(store.getStoreName());
            }
        }
        return result;
    }

    private List<StorePlatformRespVO> convertPlatformTables(List<PlatformTableDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, PlatformDO> platformMap = new HashMap<>();
        Set<Long> platformIds = list.stream()
                .map(PlatformTableDO::getPlatformId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(platformIds)) {
            List<PlatformDO> platforms = platformMapper.selectBatchIds(platformIds);
            for (PlatformDO platform : platforms) {
                platformMap.put(platform.getPlatformId(), platform);
            }
        }
        List<StorePlatformRespVO> result = new ArrayList<>(list.size());
        for (PlatformTableDO item : list) {
            StorePlatformRespVO vo = BeanUtils.toBean(item, StorePlatformRespVO.class);
            if (item.getPlatformId() != null) {
                PlatformDO platform = platformMap.get(item.getPlatformId());
                if (platform != null) {
                    vo.setPlatformName(platform.getPlatformName());
                }
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 【What】从数据库查询平台门店信息（兜底方法）
     *
     * 【Why - 为什么需要单独方法？】
     * 缓存未命中时需要查数据库，将查询逻辑抽离便于复用
     *
     * 【Constraints】
     * - 只返回关联信息，不返回平台详情（平台名称需额外查询）
     */
    private StorePlatformRespVO queryDatabaseForPlatformStoreId(String normalizedPlatformStoreId) {
        PlatformTableDO platformTable = platformTableMapper.selectOne(
                new LambdaQueryWrapperX<PlatformTableDO>()
                        .eq(PlatformTableDO::getPlatformStoreId, normalizedPlatformStoreId));
        if (platformTable == null) {
            return null;
        }
        StorePlatformRespVO vo = BeanUtils.toBean(platformTable, StorePlatformRespVO.class);
        if (platformTable.getPlatformId() != null) {
            PlatformDO platform = platformMapper.selectById(platformTable.getPlatformId());
            if (platform != null) {
                vo.setPlatformName(platform.getPlatformName());
            }
        }
        return vo;
    }

    /**
     * 【What】获取已开店门店的平台关联列表（缓存优先）
     *
     * 【查询流程】
     * 1. 从 Redis 缓存获取全部门店列表
     * 2. 缓存命中则按 storeStatus=1 过滤后转换返回
     * 3. 缓存未命中则查数据库，回填缓存
     *
     * 【Why - platformCode 参数保留但未使用？】
     * 原实现按 platformCode 筛选，但业务上需要查询所有平台
     * 当前实现查询所有已开店门店，由调用方自行过滤
     *
     * 【Constraints】
     * - 只返回有 platformStoreId 的门店
     * - 只返回 storeStatus=1（正常/开店）的门店
     *
     * 【Pitfalls】
     * - 【教训2024-03】缓存与数据库不一致导致同步任务漏掉门店
     */
    @Override
    public List<StorePlatformRespVO> getOpenPlatformStoresByPlatformCode(String platformCode) {
        List<StorePlatformInfoRespVO> cachedList = storePlatformCacheService.getStorePlatformListFromRedis();

        if (CollUtil.isNotEmpty(cachedList)) {
            List<StorePlatformInfoRespVO> openStores = cachedList.stream()
                    .filter(item -> item.getStoreStatus() != null && item.getStoreStatus() == 1)
                    .collect(Collectors.toList());
            return convertCachedListToVO(openStores);
        }

        log.info("【门店平台关联查询】Redis缓存未命中，查询数据库...");
        return queryPlatformStoresFromDatabase(platformCode, true);
    }

    @Override
    public List<StorePlatformRespVO> getAllPlatformStoresByPlatformCode(String platformCode) {
        List<StorePlatformInfoRespVO> cachedList = storePlatformCacheService.getStorePlatformListFromRedis();

        if (CollUtil.isNotEmpty(cachedList)) {
            return convertCachedListToVO(cachedList);
        }

        log.info("【门店平台关联查询-全部门店】Redis缓存未命中，查询数据库...");
        return queryPlatformStoresFromDatabase(platformCode, false);
    }

    /**
     * 【What】从数据库查询门店平台数据并直接返回结果
     *
     * 【查询流程】
     * 1. 查询门店（onlyOpen=true 时只查 storeStatus=1，否则查全部）
     * 2. 查询门店的平台关联（必须有 platformStoreId）
     * 3. 组装结果并直接返回，不在请求线程回填 Redis 缓存
     *
     * 【Why - 为什么要回填缓存？】
     * 缓存未命中说明数据过期或首次加载，需要从数据库加载后回填
     * 避免下次查询仍然穿透到数据库
     *
     * 【Pitfalls】
     * - 【教训2024-03】无 platformStoreId 的门店会被跳过，可能导致同步不完整
     * - 【Bug修复】原 storeStatus=0 条件有误，storeStatus=1 才是正常/开店
     */
    private List<StorePlatformRespVO> queryPlatformStoresFromDatabase(String platformCode, boolean onlyOpen) {
        LambdaQueryWrapperX<StoreDO> wrapper = new LambdaQueryWrapperX<StoreDO>()
                .orderByDesc(StoreDO::getStoreId);
        if (onlyOpen) {
            wrapper.eq(StoreDO::getStoreStatus, 1);
        }

        List<StoreDO> stores = storeMapper.selectList(wrapper);
        if (CollUtil.isEmpty(stores)) {
            return Collections.emptyList();
        }

        List<String> storeIds = stores.stream()
                .map(StoreDO::getStoreId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(storeIds)) {
            return Collections.emptyList();
        }

        List<PlatformTableDO> platformTables = platformTableMapper.selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .in(PlatformTableDO::getStoreId, storeIds)
                .isNotNull(PlatformTableDO::getPlatformStoreId)
                .orderByDesc(PlatformTableDO::getStoreId));
        log.info("【门店平台关联查询】onlyOpen={}, 门店数量={}, 查询到平台关联数量={}", onlyOpen, stores.size(), platformTables.size());
        if (CollUtil.isEmpty(platformTables)) {
            return Collections.emptyList();
        }

        Map<String, PlatformTableDO> platformTableMap = platformTables.stream()
                .filter(item -> StrUtil.isNotBlank(item.getStoreId()))
                .filter(item -> StrUtil.isNotBlank(item.getPlatformStoreId()))
                .collect(Collectors.toMap(PlatformTableDO::getStoreId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));

        List<StorePlatformRespVO> result = new ArrayList<>();
        for (StoreDO store : stores) {
            PlatformTableDO platformTable = platformTableMap.get(store.getStoreId());
            if (platformTable == null) {
                log.info("【门店平台关联查询】门店缺少 platform_store_id, storeId={}, storeName={}", store.getStoreId(),
                        store.getStoreName());
                continue;
            }
            StorePlatformRespVO vo = BeanUtils.toBean(platformTable, StorePlatformRespVO.class);
            if (platformTable.getPlatformId() != null) {
                PlatformDO platform = platformMapper.selectById(platformTable.getPlatformId());
                if (platform != null) {
                    vo.setPlatformName(platform.getPlatformName());
                }
            }
            result.add(vo);
        }

        if (CollUtil.isNotEmpty(result)) {
            log.info("【门店平台关联查询】数据库查询完成，当前直接返回结果，不在请求线程刷新Redis缓存，共{}条", result.size());
        }

        return result;
    }

    /**
     * 【What】将 Redis 缓存数据转换为 VO
     *
     * 【Why - 为什么需要转换？】
     * Redis 缓存的 StorePlatformInfoRespVO 与返回的 StorePlatformRespVO 结构不同
     * 缓存对象包含 storeId/storeName，返回对象包含 platformStoreId/platformStoreName
     *
     * 【Constraints】
     * - 只转换必要字段，避免泄漏缓存内部结构
     */
    private List<StorePlatformRespVO> convertCachedListToVO(List<StorePlatformInfoRespVO> cachedList) {
        if (CollUtil.isEmpty(cachedList)) {
            return Collections.emptyList();
        }

        List<StorePlatformRespVO> result = new ArrayList<>();
        for (StorePlatformInfoRespVO cached : cachedList) {
            StorePlatformRespVO vo = new StorePlatformRespVO();
            vo.setPlatformStoreId(cached.getPlatformStoreId());
            vo.setPlatformStoreName(cached.getStoreName());
            result.add(vo);
        }
        return result;
    }

    /**
     * 【What】获取缓存中的门店平台信息
     * 【Constraints】依赖 syncStorePlatformInfo() 刷新，返回可能为空
     */
    @Override
    public List<StorePlatformInfoRespVO> getStorePlatformInfoList() {
        return storePlatformCacheService.getStorePlatformListFromRedis();
    }

    /**
     * 【What】手动触发门店平台缓存同步
     * 【Constraints】保持同步调用语义，供运维修复与缓存预热使用
     */
    @Override
    public void syncStorePlatformInfo() {
        storePlatformCacheService.syncStorePlatformInfoToRedis();
    }

}