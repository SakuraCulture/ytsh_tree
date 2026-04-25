package cn.iocoder.yudao.module.business.service.store;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

/**
 * 门店商品 Service 实现类
 *
 * ==============================================================
 * 【Why - 核心设计决策】
 * ==============================================================
 *
 * 1. 为什么要校验入店商品唯一性？
 *    → 同一门店的同一 SKU 只能有一条入店记录
 *    → 避免重复入店导致库存计算混乱
 *
 * 2. 为什么要检查库存才能删除？
 *    → 删除前检查库存不为零
 *    → 避免删除后库存数据孤儿
 *
 * 3. 为什么要自动设置首次入店日期和上架时间？
 *    → 便于追踪商品上下架历史
 *    → 业务统计需要
 *
 * ==============================================================
 * 【What - 核心功能】
 * ==============================================================
 * - 门店商品 CRUD 操作
 * - 入店商品唯一性校验
 * - 库存前置检查
 * - 批量导入功能
 *
 * ==============================================================
 * 【Constraints - 约束条件】
 * ==============================================================
 * - 入店商品唯一性：(storeId + productSkuId + 入店) 组合唯一
 * - 删除前检查库存不为零
 * - 写操作使用 @Transactional(rollbackFor = Exception.class)
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【教训2024-07】只校验了入店归属，其他归属类型未校验
 * - 【教训2024-08】库存同步时机问题导致数据不一致
 * - 【陷阱】分页查询时批量查询关联数据可能导致 N+1 问题
 *
 * @author 彼岸花
 */
@Service
@Validated
public class StoreProductServiceImpl implements StoreProductService {

    @Resource
    private StoreProductMapper storeProductMapper;
    @Resource
    private StoreStockMapper storeStockMapper;
    @Resource
    private StoreMapper storeMapper;
    @Resource
    private SkuTableMapper skuTableMapper;

    /**
     * 入店归属常量
     * 用于校验入店商品唯一性
     */
    private static final String OWNERSHIP_IN = "入店";

    /**
     * 创建门店商品
     *
     * 【What】
     * 1. 校验同一门店的同一 SKU + 入店归属是否已存在
     * 2. 插入门店商品记录
     * 3. 自动设置首次入店日期和上架时间
     *
     * 【Why - 为什么要自动设置日期？】
     * - 便于追踪商品上下架历史
     * - 业务统计需要
     *
     * 【Constraints】
     * - 入店商品必须唯一：(storeId + productSkuId + 入店) 不能重复
     * - 自动设置首次入店日期和上架时间
     *
     * 【Pitfalls】
     * - 【陷阱】只校验了入店归属，其他归属类型未校验
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createStoreProduct(StoreProductSaveReqVO createReqVO) {
        // 校验入店商品唯一性
        if (OWNERSHIP_IN.equals(createReqVO.getStoreProductOwnership())) {
            StoreProductDO existProduct = storeProductMapper.selectByStoreIdAndProductSkuIdAndOwnership(
                    createReqVO.getStoreId(), createReqVO.getProductSkuId(), createReqVO.getStoreProductOwnership());
            if (existProduct != null) {
                throw exception(STORE_PRODUCT_SKU_EXISTS_IN_STORE);
            }
        }

        StoreProductDO storeProduct = BeanUtils.toBean(createReqVO, StoreProductDO.class);
        // 自动设置首次入店日期
        if (storeProduct.getStoreProductFirstDate() == null) {
            storeProduct.setStoreProductFirstDate(LocalDate.now());
        }
        // 自动设置上架时间
        storeProduct.setStoreProductShelfTime(LocalDateTime.now());
        storeProductMapper.insert(storeProduct);
        return storeProduct.getStoreProductId();
    }

    /**
     * 更新门店商品
     *
     * 【What】
     * 1. 校验商品存在性
     * 2. 检查同一门店的同一 SKU + 入店归属是否被其他商品占用
     * 3. 更新门店商品
     *
     * 【Constraints】
     * - 更新前校验商品存在性
     *
     * @param updateReqVO 更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreProduct(StoreProductSaveReqVO updateReqVO) {
        validateStoreProductExists(updateReqVO.getStoreProductId());

        if (OWNERSHIP_IN.equals(updateReqVO.getStoreProductOwnership())) {
            StoreProductDO existProduct = storeProductMapper.selectByStoreIdAndProductSkuIdAndOwnership(
                    updateReqVO.getStoreId(), updateReqVO.getProductSkuId(), updateReqVO.getStoreProductOwnership());
            if (existProduct != null && !existProduct.getStoreProductId().equals(updateReqVO.getStoreProductId())) {
                throw exception(STORE_PRODUCT_SKU_EXISTS_IN_STORE);
            }
        }

        StoreProductDO updateObj = BeanUtils.toBean(updateReqVO, StoreProductDO.class);
        storeProductMapper.updateById(updateObj);
    }

    /**
     * 删除门店商品
     *
     * 【What】
     * 1. 校验商品存在性
     * 2. 检查库存不为零
     * 3. 删除门店商品
     *
     * 【Why - 为什么要检查库存？】
     * - 避免删除后库存数据孤儿
     * - 保证数据完整性
     *
     * 【Constraints】
     * - 库存不为零时不能删除
     *
     * 【Pitfalls】
     * - 【陷阱】库存检查可能导致删除失败，需先处理库存
     *
     * @param id 编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStoreProduct(String id) {
        validateStoreProductExists(id);

        // 检查库存
        StoreStockRespVO stock = getStoreStockByStoreProductId(id);
        if (stock != null && stock.getInventoryQuantity() != null && stock.getInventoryQuantity() > 0) {
            throw exception(STORE_PRODUCT_DELETE_HAS_STOCK);
        }

        storeProductMapper.deleteById(id);
    }

    /**
     * 批量删除门店商品
     *
     * 【What】
     * 逐条校验库存后批量删除
     *
     * 【Why - 为什么要逐条校验？】
     * - 每条商品都需检查库存
     * - 任何一条库存不为零都会导致整体失败
     *
     * @param ids 编号列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStoreProductList(List<String> ids) {
        for (String id : ids) {
            deleteStoreProduct(id);
        }
    }

    /**
     * 校验门店商品是否存在
     *
     * 【What】
     * 根据 ID 查询门店商品，不存在则抛异常
     *
     * @param id 编号
     * @return 门店商品DO
     * @throws 业务异常：商品不存在
     */
    private StoreProductDO validateStoreProductExists(String id) {
        StoreProductDO storeProduct = storeProductMapper.selectById(id);
        if (storeProduct == null) {
            throw exception(STORE_PRODUCT_NOT_EXISTS);
        }
        return storeProduct;
    }

    /**
     * 获得门店商品
     *
     * 【What】
     * 根据 ID 查询门店商品详情，关联门店和 SKU 信息
     *
     * 【Constraints】
     * - 返回 null 表示不存在
     *
     * @param id 编号
     * @return 门店商品信息
     */
    @Override
    public StoreProductRespVO getStoreProduct(String id) {
        StoreProductDO storeProduct = storeProductMapper.selectById(id);
        if (storeProduct == null) {
            return null;
        }
        return buildRespVO(storeProduct,
                getStoreMap(Collections.singleton(storeProduct.getStoreId())),
                getSkuMap(Collections.singleton(storeProduct.getProductSkuId())));
    }

    /**
     * 获得门店商品分页
     *
     * 【What】
     * 支持分页、关键词搜索的门店商品列表查询
     *
     * 【处理流程】
     * 1. 根据 SKU 关键词查询匹配的 SKU ID 列表
     * 2. 分页查询门店商品
     * 3. 批量查询关联的门店和 SKU 信息
     * 4. 组装响应数据
     *
     * 【Why - 为什么要批量查询关联数据？】
     * - 避免 N+1 查询问题
     * - 提高查询性能
     *
     * 【Constraints】
     * - pageReqVO 需包含分页参数
     *
     * @param pageReqVO 分页查询
     * @return 门店商品分页
     */
    @Override
    public PageResult<StoreProductRespVO> getStoreProductPage(StoreProductPageReqVO pageReqVO) {
        List<String> productSkuIds = getProductSkuIds(pageReqVO);
        if (hasSkuFilter(pageReqVO) && CollUtil.isEmpty(productSkuIds)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        PageResult<StoreProductDO> pageResult = storeProductMapper.selectPage(pageReqVO, productSkuIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询关联数据，避免 N+1 问题
        Map<String, StoreDO> storeMap = getStoreMap(pageResult.getList().stream()
                .map(StoreProductDO::getStoreId)
                .collect(Collectors.toSet()));
        Map<String, SkuTableDO> skuMap = getSkuMap(pageResult.getList().stream()
                .map(StoreProductDO::getProductSkuId)
                .collect(Collectors.toSet()));
        List<StoreProductRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, storeMap, skuMap))
                .collect(Collectors.toList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    /**
     * 获得门店商品简单列表
     *
     * 【What】
     * 返回门店商品的简化信息
     *
     * @param storeId 门店ID
     * @return 门店商品简单列表
     */
    @Override
    public List<StoreProductSimpleRespVO> getStoreProductSimpleList(String storeId) {
        List<StoreProductDO> list = storeProductMapper.selectListByStoreId(storeId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, StoreProductSimpleRespVO.class);
    }

    /**
     * 获得所有门店商品简单列表
     *
     * 【What】
     * 返回所有门店商品的简化信息
     *
     * @return 所有门店商品简单列表
     */
    @Override
    public List<StoreProductSimpleRespVO> getAllStoreProductSimpleList() {
        List<StoreProductDO> list = storeProductMapper.selectListAll();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, StoreProductSimpleRespVO.class);
    }

    /**
     * 获得门店库存
     *
     * 【What】
     * 根据门店商品ID查询库存信息
     *
     * 【Constraints】
     * - 返回 null 表示不存在
     *
     * @param storeProductId 门店商品ID
     * @return 门店库存
     */
    @Override
    public StoreStockRespVO getStoreStockByStoreProductId(String storeProductId) {
        StoreStockDO stock = storeStockMapper.selectOne(new LambdaQueryWrapperX<StoreStockDO>()
                .eq(StoreStockDO::getStoreProductId, storeProductId));
        if (stock == null) {
            return null;
        }
        StoreStockRespVO resp = new StoreStockRespVO();
        resp.setAvailableQuantity(stock.getStoreStockAvailableQuantity());
        resp.setInventoryQuantity(stock.getStoreStockQuantity());
        resp.setInTransitQuantity(stock.getStoreStockTransitQuantity());
        resp.setFrozenQuantity(stock.getStoreStockFrozenQuantity());
        resp.setOutOfStockDuration(stock.getStoreStockOutstockHours());
        return resp;
    }

    /**
     * 导入门店商品列表
     *
     * 【What】
     * 批量导入门店商品，支持新增和更新两种模式
     *
     * 【处理流程】
     * 1. 遍历导入数据
     * 2. 检查是否存在（按 storeId + skuId + 入店归属判断）
     * 3. 不存在则新增，存在则根据 isUpdateSupport 决定更新或跳过
     * 4. 汇总成功/失败结果
     *
     * 【Why - 为什么要校验入店归属？】
     * - 入店商品唯一性约束
     * - 避免重复导入
     *
     * 【Constraints】
     * - 必须使用 @Transactional(rollbackFor = Exception.class)
     * - 返回结果包含成功/失败列表
     *
     * 【Pitfalls】
     * - 【陷阱】只校验了入店归属，其他归属类型未校验
     * - 【陷阱】forEach 内 try-catch 不会自动回滚事务
     *
     * @param importList 导入列表
     * @param isUpdateSupport 是否支持更新
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreProductImportRespVO importStoreProductList(List<StoreProductImportExcelVO> importList, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importList)) {
            throw exception(STORE_PRODUCT_IMPORT_LIST_IS_EMPTY);
        }

        StoreProductImportRespVO respVO = StoreProductImportRespVO.builder()
                .createStoreProductIds(new ArrayList<>())
                .updateStoreProductIds(new ArrayList<>())
                .failureStoreProductIds(new LinkedHashMap<>())
                .build();

        importList.forEach(importItem -> {
            try {
                StoreProductDO existProduct = null;
                if (StrUtil.isNotBlank(importItem.getStoreId()) && StrUtil.isNotBlank(importItem.getProductSkuId())) {
                    existProduct = storeProductMapper.selectByStoreIdAndProductSkuIdAndOwnership(
                            importItem.getStoreId(), importItem.getProductSkuId(), importItem.getStoreProductOwnership());
                }

                if (existProduct == null) {
                    StoreProductDO storeProduct = BeanUtils.toBean(importItem, StoreProductDO.class);
                    if (storeProduct.getStoreProductFirstDate() == null) {
                        storeProduct.setStoreProductFirstDate(LocalDate.now());
                    }
                    storeProduct.setStoreProductShelfTime(LocalDateTime.now());
                    storeProductMapper.insert(storeProduct);
                    respVO.getCreateStoreProductIds().add(storeProduct.getStoreProductId());
                } else if (isUpdateSupport) {
                    StoreProductDO updateProduct = BeanUtils.toBean(importItem, StoreProductDO.class);
                    updateProduct.setStoreProductId(existProduct.getStoreProductId());
                    storeProductMapper.updateById(updateProduct);
                    respVO.getUpdateStoreProductIds().add(existProduct.getStoreProductId());
                } else {
                    respVO.getFailureStoreProductIds().put(
                            importItem.getStoreId() + "_" + importItem.getProductSkuId(),
                            "门店商品已存在，不允许重复导入");
                }
            } catch (Exception ex) {
                String key = (importItem.getStoreId() != null && !importItem.getStoreId().isEmpty())
                        ? importItem.getStoreId() + "_" + importItem.getProductSkuId()
                        : "第 " + (importList.indexOf(importItem) + 1) + " 行";
                respVO.getFailureStoreProductIds().put(key, ex.getMessage());
            }
        });

        return respVO;
    }

    /**
     * 获得SKU简单列表
     *
     * 【What】
     * 返回所有 SKU 的简化信息
     *
     * @return SKU简单列表
     */
    @Override
    public List<SkuSimpleRespVO> getSkuSimpleList() {
        List<SkuTableDO> list = skuTableMapper.selectAllSimpleList();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, SkuSimpleRespVO.class);
    }

    /**
     * 检查是否有 SKU 关键词筛选
     */
    private boolean hasSkuFilter(StoreProductPageReqVO pageReqVO) {
        return StrUtil.isNotBlank(pageReqVO.getSkuCode()) || StrUtil.isNotBlank(pageReqVO.getSkuName());
    }

    /**
     * 根据关键词获取 SKU ID 列表
     *
     * 【Why - 为什么要先查 SKU ID 列表？】
     * - SKU 筛选需要先匹配 SKU 信息
     * - 减少门店商品表的查询范围
     */
    private List<String> getProductSkuIds(StoreProductPageReqVO pageReqVO) {
        // 精确匹配 productSkuId
        if (StrUtil.isNotBlank(pageReqVO.getProductSkuId())) {
            return Collections.singletonList(pageReqVO.getProductSkuId());
        }
        // 无 SKU 筛选条件
        if (!hasSkuFilter(pageReqVO)) {
            return null;
        }
        // 关键词筛选 SKU
        List<SkuTableDO> skuList = skuTableMapper.selectListByKeyword(pageReqVO.getSkuCode(), pageReqVO.getSkuName());
        if (CollUtil.isEmpty(skuList)) {
            return Collections.emptyList();
        }
        return skuList.stream()
                .map(SkuTableDO::getProductSkuId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 批量获取门店信息
     *
     * 【Why - 为什么要批量查询？】
     * - 避免 N+1 查询问题
     * - 提高分页查询性能
     */
    private Map<String, StoreDO> getStoreMap(Collection<String> storeIds) {
        if (CollUtil.isEmpty(storeIds)) {
            return Collections.emptyMap();
        }
        return storeMapper.selectList(new LambdaQueryWrapperX<StoreDO>()
                        .inIfPresent(StoreDO::getStoreId, storeIds))
                .stream()
                .collect(Collectors.toMap(StoreDO::getStoreId, Function.identity(), (item1, item2) -> item1));
    }

    /**
     * 批量获取 SKU 信息
     *
     * 【Why - 为什么要批量查询？】
     * - 避免 N+1 查询问题
     */
    private Map<String, SkuTableDO> getSkuMap(Collection<String> productSkuIds) {
        if (CollUtil.isEmpty(productSkuIds)) {
            return Collections.emptyMap();
        }
        return skuTableMapper.selectListByProductSkuIds(productSkuIds)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getProductSkuId()), Function.identity(), (item1, item2) -> item1));
    }

    /**
     * 组装响应 VO
     *
     * 【What】
     * 将 DO 转换为 RespVO，填充关联信息
     */
    private StoreProductRespVO buildRespVO(StoreProductDO storeProduct,
                                           Map<String, StoreDO> storeMap,
                                           Map<String, SkuTableDO> skuMap) {
        StoreProductRespVO respVO = new StoreProductRespVO();
        respVO.setStoreProductId(storeProduct.getStoreProductId());
        respVO.setStoreId(storeProduct.getStoreId());
        respVO.setProductSkuId(storeProduct.getProductSkuId());
        respVO.setProductAttribution(storeProduct.getStoreProductOwnership());
        respVO.setPosStatus(parseInteger(storeProduct.getStoreProductPosStatus()));
        respVO.setStoreRetailPrice(storeProduct.getStoreProductPrice());
        respVO.setEnterShopStatus(storeProduct.getStoreProductIsActive());
        respVO.setFirstEnterShopDate(storeProduct.getStoreProductFirstDate());
        respVO.setCreateTime(storeProduct.getCreateTime());

        // 填充门店信息
        StoreDO store = storeMap.get(storeProduct.getStoreId());
        if (store != null) {
            respVO.setStoreName(store.getStoreName());
        }

        // 填充 SKU 信息
        SkuTableDO sku = skuMap.get(storeProduct.getProductSkuId());
        if (sku != null) {
            respVO.setSkuCode(sku.getProductSkuCode());
            respVO.setSkuName(sku.getProductSkuName());
        }
        return respVO;
    }

    /**
     * 解析整数
     *
     * 【What】
     * 将字符串解析为整数，失败返回 null
     */
    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
