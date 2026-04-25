package cn.iocoder.yudao.module.business.dal.redis.store;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformInfoRespVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.module.business.dal.redis.BusinessRedisKeyConstants.STORE_PLATFORM_INFO;

/**
 * 门店平台信息 Redis DAO
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
 *   1. 每次查询都需要 JOIN，SQL 开销大
 *   2. 门店平台查询是高频操作
 *
 * 方案B（Redis 缓存）：
 * - 优点：查询从 O(JOIN) 降为 O(1)
 * - 缺点：数据可能不一致
 *
 * 最终选择：方案B
 * - 业务上确实需要高频查询
 * - 门店变更频率较低，适合缓存
 *
 * ==============================================================
 * 【Why - 为什么要区分 null 和空列表？】
 * ==============================================================
 *
 * null 和空列表代表不同的含义：
 * - null：缓存不存在，需要回源查询数据库
 * - 空列表：缓存存在，但查询结果为空（门店存在但无平台关联）
 *
 * 如果返回空列表表示"缓存为空"，则无法区分"未缓存"和"缓存为空"，
 * 导致每次都回源查询数据库，增加数据库压力。
 *
 * ==============================================================
 * 【Why - 为什么要覆盖而不是追加？】
 * ==============================================================
 *
 * 追加 vs 覆盖：
 * - 追加：需要先 get、再合并、再 set，操作非原子
 * - 覆盖：直接 set，操作原子，更简单
 *
 * 选择覆盖的原因：
 * 1. 门店平台信息是全量数据，每次同步都是全量覆盖
 * 2. 避免并发操作导致数据不一致
 * 3. 实现简单，bug 少
 *
 * ==============================================================
 * 【What - 这个类做什么】
 * ==============================================================
 * - 封装门店平台关联信息的 Redis 操作
 * - 提供序列化/反序列化能力
 * - 缓存 Key 管理
 *
 * ==============================================================
 * 【Constraints - 约束与限制】
 * ==============================================================
 * - 缓存无 TTL 限制，依赖主动同步
 * - 使用 String 类型存储 JSON，简化序列化逻辑
 * - 数据变更时必须同步更新缓存
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【教训2024-02】初始设计使用 List 存储，导致序列化开销大
 *   → 修复：优化数据结构
 * - 【陷阱】Redis 宕机时，查询会回源到数据库，可能导致雪崩
 *   → 缓解：使用分布式锁控制同步
 * - 【风险】缓存数据与服务端不一致时，难以排查
 *   → 提供 sync 接口手动同步
 * - 【边界】空列表 vs null → 空列表是有效数据，null 表示未缓存
 *
 * @author 彼岸花
 * @see StorePlatformCacheService
 * @see BusinessRedisKeyConstants
 */
@Repository
public class StorePlatformRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取门店平台信息列表
     *
     * 【What】从 Redis 获取缓存的门店平台信息列表
     *
     * 【Return - 返回值约定】
     * - 非 null：缓存命中，返回反序列化后的列表
     * - null：缓存未命中，上层会回源查询数据库
     *
     * 【Why - 为什么要返回 null 而不是空列表？】
     * - null 表示缓存不存在，需要回源查询
     * - 空列表表示"有数据但为空"
     * - 两者含义不同，必须区分
     *
     * 【Pitfalls】
     * - 【陷阱】JSON 解析失败会抛出异常，需要上层捕获
     * - 【边界】Redis Key 不存在时，get 返回 null
     *
     * @return 门店平台信息列表，若缓存不存在返回 null
     */
    public List<StorePlatformInfoRespVO> getStorePlatformList() {
        String json = stringRedisTemplate.opsForValue().get(STORE_PLATFORM_INFO);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonUtils.parseArray(json, StorePlatformInfoRespVO.class);
    }

    /**
     * 设置门店平台信息列表
     *
     * 【What】将门店平台信息列表写入 Redis 缓存
     *
     * 【Why - 为什么要覆盖而不是追加？】
     * - 门店平台信息是全量数据，每次同步都是全量覆盖
     * - 覆盖操作原子，更简单可靠
     *
     * 【Constraints】
     * - 直接覆盖原值，无追加逻辑
     * - 空列表表示"有数据但为空"，会覆盖掉之前的缓存
     *
     * 【Pitfalls】
     * - 【陷阱】空列表会清空之前的缓存
     *   → 确保只在有数据时才调用此方法
     *
     * @param list 门店平台信息列表
     */
    public void setStorePlatformList(List<StorePlatformInfoRespVO> list) {
        String json = JsonUtils.toJsonString(list);
        stringRedisTemplate.opsForValue().set(STORE_PLATFORM_INFO, json);
    }

    /**
     * 删除门店平台信息缓存
     *
     * 【What】清空 Redis 缓存
     *
     * 【Why - 什么时候需要删除？】
     * - 数据异常时需要重置
     * - 系统重置场景
     * - 缓存数据与服务端不一致时
     */
    public void deleteStorePlatformList() {
        stringRedisTemplate.delete(STORE_PLATFORM_INFO);
    }

    /**
     * 检查缓存是否存在
     *
     * 【What】检查 Redis 中是否存在门店平台信息缓存
     *
     * 【UseCase】
     * - 用于判断缓存是否已初始化
     * - 配合 getStorePlatformList() 使用
     *
     * @return true-存在，false-不存在
     */
    public boolean hasStorePlatformList() {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(STORE_PLATFORM_INFO));
    }

}
