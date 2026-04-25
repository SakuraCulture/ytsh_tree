package cn.iocoder.yudao.module.business.service.store;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformInfoRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.redis.store.StorePlatformRedisDAO;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.business.dal.redis.BusinessRedisKeyConstants.STORE_PLATFORM_INFO;

/**
 * 门店平台信息缓存 Service
 *
 * ==============================================================
 * 【Why - 为什么要使用 Redis 缓存】
 * ==============================================================
 *
 * 设计决策：为什么要缓存门店平台关联信息？
 *
 * 方案A（直接查数据库）：
 * - 优点：数据实时一致
 * - 缺点：
 * 1. 每次查询都需要 JOIN store_table 和 platform_table，SQL 开销大
 * 2. 门店平台查询是高频操作（订单系统、商品系统都要用）
 * 3. 千万级数据下，即使有索引也有性能瓶颈
 *
 * 方案B（Redis 缓存）：
 * - 优点：
 * 1. 查询从 O(JOIN) 降为 O(1)
 * 2. 扛住突发流量
 * 3. 读写分离架构下，从库压力大减
 * - 缺点：
 * 1. 数据可能不一致
 * 2. 引入额外组件复杂度
 *
 * 最终选择：方案B
 * - 业务上确实需要高频查询
 * - 门店变更频率较低，适合缓存
 * - 提供手动同步接口，保证数据一致性
 *
 * ==============================================================
 * 【Why - 为什么不使用本地缓存（如 Caffeine）？】
 * ==============================================================
 * - 分布式场景下，本地缓存无法保证多节点一致性
 * - 同一门店在不同节点的缓存可能不同
 * - Redis 作为分布式缓存，可以保证各节点数据一致
 *
 * ==============================================================
 * 【Why - 为什么要使用分布式锁？】
 * ==============================================================
 * - 多个节点可能同时触发同步操作
 * - 无锁情况下可能产生重复同步、浪费资源
 * - 分布式锁保证同一时刻只有一个节点执行同步
 *
 * ==============================================================
 * 【What - 这个类做什么】
 * ==============================================================
 * - 封装门店平台关联信息的 Redis 缓存操作
 * - 提供同步机制，确保缓存与数据库一致
 * - 使用 Redisson 分布式锁控制并发
 *
 * ==============================================================
 * 【Constraints - 约束与限制】
 * ==============================================================
 * - 缓存无 TTL 限制，依赖主动同步
 * - 使用 String 类型存储 JSON，简化序列化逻辑
 * - 数据变更时必须同步更新缓存
 * - 分布式锁获取超时 10 秒，持有超时 30 秒
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【教训2024-02】初始设计使用 List 存储，导致序列化开销大
 * → 修复：优化数据结构
 * - 【陷阱】Redis 宕机时，查询会回源到数据库，可能导致雪崩
 * → 缓解：使用 Redisson 分布式锁
 * - 【风险】缓存数据与服务端不一致时，难以排查
 * → 提供 sync 接口手动同步
 * - 【边界】空列表 vs null → 空列表是有效数据，null 表示未缓存
 *
 * @author 彼岸花
 * @see StorePlatformRedisDAO
 */
@Slf4j
@Service
public class StorePlatformCacheService {

    @Resource
    private StoreMapper storeMapper;
    @Resource
    private PlatformTableMapper platformTableMapper;
    @Resource
    private PlatformMapper platformMapper;
    @Resource
    private StorePlatformRedisDAO storePlatformRedisDAO;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 分布式锁 Key
     */
    private static final String SYNC_LOCK_KEY = "store_platform_sync_lock";

    /**
     * 同步门店平台信息到 Redis
     *
     * 【What】
     * 将数据库中的门店平台关联信息同步到 Redis 缓存
     *
     * 【Why - 为什么要用分布式锁？】
     * - 多个节点可能同时触发同步操作
     * - 无锁情况下可能产生重复同步、浪费资源
     * - tryLock(10, 30) 表示：10秒内获取锁，获取后持有30秒自动释放
     *
     * 【Constraints】
     * - 使用 Redisson 分布式锁
     * - 获取锁失败时跳过本次同步，避免阻塞
     *
     * 【Pitfalls】
     * - 【陷阱】获取分布式锁被中断时，需要恢复中断状态
     * - 【边界】获取锁失败时，记录警告日志但不抛异常
     */
    public void syncStorePlatformInfoToRedis() {
        RLock lock = redissonClient.getLock(SYNC_LOCK_KEY);
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    doSyncStorePlatformInfoToRedis();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("【门店平台信息同步】获取分布式锁失败，跳过本次同步");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【门店平台信息同步】获取分布式锁被中断", e);
        }
    }

    /**
     * 执行同步逻辑
     *
     * 【What】
     * 实际执行缓存同步的核心逻辑
     *
     * 【处理流程】
     * 1. 查询所有已开店的门店（store_status=0）
     * 2. 查询所有门店的平台关联信息
     * 3. 组装数据并写入 Redis
     *
     * 【Constraints】
     * - 只同步已开店的门店
     * - 只同步有 platformStoreId 的关联
     * - 无数据时清空缓存
     *
     * 【Pitfalls】
     * - 【教训2024-02】未过滤无 platformStoreId 的关联
     * → 修复：添加 isNotNull(PlatformTableDO::getPlatformStoreId) 条件
     * - 【边界】storeIds 为空时清空缓存
     */
    private void doSyncStorePlatformInfoToRedis() {
        log.info("【门店平台信息同步】开始同步门店平台信息到Redis,/admin-api/business/table/platform-info/sync");

        // 1. 查询所有已开店的门店（storeStatus=1 表示正常/开店）
        List<StoreDO> stores = storeMapper.selectList(new LambdaQueryWrapperX<StoreDO>()
                .eq(StoreDO::getStoreStatus, 1)
                .orderByDesc(StoreDO::getStoreId));
        if (CollUtil.isEmpty(stores)) {
            log.info("【门店平台信息同步】未查询到已开店的门店，清空Redis缓存");
            storePlatformRedisDAO.deleteStorePlatformList();
            return;
        }

        List<String> storeIds = stores.stream()
                .map(StoreDO::getStoreId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(storeIds)) {
            log.info("【门店平台信息同步】门店ID列表为空，清空Redis缓存");
            storePlatformRedisDAO.deleteStorePlatformList();
            return;
        }

        // 2. 查询平台关联信息
        List<PlatformTableDO> platformTables = platformTableMapper.selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .in(PlatformTableDO::getStoreId, storeIds)
                .isNotNull(PlatformTableDO::getPlatformStoreId)
                .orderByDesc(PlatformTableDO::getStoreId));
        if (CollUtil.isEmpty(platformTables)) {
            log.info("【门店平台信息同步】未查询到平台关联信息，清空Redis缓存");
            storePlatformRedisDAO.deleteStorePlatformList();
            return;
        }

        // 3. 构建 storeId -> PlatformTable 映射
        Map<String, PlatformTableDO> platformTableMap = platformTables.stream()
                .filter(item -> StrUtil.isNotBlank(item.getStoreId()))
                .filter(item -> StrUtil.isNotBlank(item.getPlatformStoreId()))
                .collect(Collectors.toMap(PlatformTableDO::getStoreId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));

        // 4. 构建 platformId -> Platform 映射
        Map<Long, PlatformDO> platformMap = new HashMap<>();
        Set<Long> platformIds = platformTables.stream()
                .map(PlatformTableDO::getPlatformId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(platformIds)) {
            List<PlatformDO> platforms = platformMapper.selectBatchIds(platformIds);
            for (PlatformDO platform : platforms) {
                platformMap.put(platform.getPlatformId(), platform);
            }
        }

        // 5. 组装结果并写入缓存
        List<StorePlatformInfoRespVO> result = new ArrayList<>();
        for (StoreDO store : stores) {
            PlatformTableDO platformTable = platformTableMap.get(store.getStoreId());
            if (platformTable == null) {
                continue;
            }
            StorePlatformInfoRespVO vo = new StorePlatformInfoRespVO();
            vo.setPlatformStoreId(StrUtil.trim(platformTable.getPlatformStoreId()));
            vo.setStoreName(StrUtil.trim(store.getStoreName()));
            result.add(vo);
        }

        storePlatformRedisDAO.setStorePlatformList(result);
        log.info("【门店平台信息同步】同步完成，共 {} 条门店平台信息", result.size());
    }

    /**
     * 从 Redis 获取门店平台信息列表
     *
     * 【What】从 Redis 获取缓存的门店平台信息
     *
     * 【Constraints】
     * - 依赖 syncStorePlatformInfoToRedis() 定期刷新缓存
     * - 缓存为空时返回空列表，调用方需处理降级逻辑
     *
     * @return 门店平台信息列表
     */
    public List<StorePlatformInfoRespVO> getStorePlatformListFromRedis() {
        return storePlatformRedisDAO.getStorePlatformList();
    }

    /**
     * 删除门店平台信息缓存
     *
     * 【What】清空 Redis 缓存
     *
     * 【Why - 什么时候需要删除缓存？】
     * - 数据异常时需要重置
     * - 系统重置场景
     *
     * @return void
     */
    public void deleteStorePlatformList() {
        storePlatformRedisDAO.deleteStorePlatformList();
    }

}
