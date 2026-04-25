package cn.iocoder.yudao.module.business.service.store;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * 门店商品 Service 接口
 *
 * ==============================================================
 * 【Why - 为什么要单独管理门店商品】
 * ==============================================================
 *
 * 设计决策：为什么要独立于通用商品管理门店商品？
 *
 * 方案A（通用商品表）：所有商品使用同一张表
 * - 优点：统一管理，查询简单
 * - 缺点：
 *   1. 门店商品有特殊的归属、价格、上架状态等属性
 *   2. 不同门店可能有不同的商品配置
 *   3. 门店商品与库存、订单有强关联
 *
 * 方案B（独立表）：拆出 store_product 表
 * - 优点：
 *   1. 支持门店维度的商品管理
 *   2. 便于门店独立定价、上架
 *   3. 支持门店商品与通用商品解耦
 * - 缺点：需要关联查询
 *
 * 最终选择：方案B
 * - 业务上门店需要独立管理商品配置
 * - 支持多门店、多商品、多价格场景
 *
 * ==============================================================
 * 【What - 这个接口做什么】
 * ==============================================================
 * - 门店商品的 CRUD 操作
 * - 门店商品的导入导出
 * - 门店库存查询
 *
 * ==============================================================
 * 【Constraints - 约束条件】
 * ==============================================================
 * - 唯一性：(storeId + productSkuId + ownership) 组合唯一
 * - 删除前检查：库存不为零时不能删除
 * - 事务要求：写操作必须使用 @Transactional(rollbackFor = Exception.class)
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【陷阱】入店商品唯一性校验只校验了入店归属
 * - 【陷阱】库存同步时机问题可能导致数据不一致
 *
 * @author 彼岸花
 */
public interface StoreProductService {

    /**
     * 创建门店商品
     *
     * 【What】
     * 1. 校验同一门店的同一 SKU + 入店归属是否已存在
     * 2. 插入门店商品记录
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
    String createStoreProduct(@Valid StoreProductSaveReqVO createReqVO);

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
    void updateStoreProduct(@Valid StoreProductSaveReqVO updateReqVO);

    /**
     * 删除门店商品
     *
     * 【What】
     * 1. 校验商品存在性
     * 2. 检查库存不为零
     * 3. 删除门店商品
     *
     * 【Constraints】
     * - 库存不为零时不能删除
     *
     * 【Pitfalls】
     * - 【陷阱】库存检查可能导致删除失败，需先处理库存
     *
     * @param id 编号
     */
    void deleteStoreProduct(String id);

    /**
     * 批量删除门店商品
     *
     * 【What】批量删除门店商品，逐条校验库存
     *
     * 【Constraints】
     * - 任何一条库存不为零都会导致整体失败
     *
     * @param ids 编号列表
     */
    void deleteStoreProductList(List<String> ids);

    /**
     * 获得门店商品
     *
     * 【What】
     * 根据 ID 查询门店商品详情
     *
     * @param id 编号
     * @return 门店商品
     */
    StoreProductRespVO getStoreProduct(String id);

    /**
     * 获得门店商品分页
     *
     * 【What】
     * 支持分页、排序、关键词搜索的门店商品列表查询
     *
     * 【Constraints】
     * - pageReqVO 需包含分页参数
     * - 支持按 SKU 关键词筛选
     *
     * @param pageReqVO 分页查询
     * @return 门店商品分页
     */
    PageResult<StoreProductRespVO> getStoreProductPage(StoreProductPageReqVO pageReqVO);

    /**
     * 获得门店商品简单列表
     *
     * 【What】
     * 返回门店商品的简化信息，用于下拉选择等轻量场景
     *
     * @param storeId 门店ID
     * @return 门店商品简单列表
     */
    List<StoreProductSimpleRespVO> getStoreProductSimpleList(String storeId);

    /**
     * 获得所有门店商品简单列表
     *
     * 【What】
     * 返回所有门店商品的简化信息
     *
     * @return 所有门店商品简单列表
     */
    List<StoreProductSimpleRespVO> getAllStoreProductSimpleList();

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
    StoreStockRespVO getStoreStockByStoreProductId(String storeProductId);

    /**
     * 导入门店商品列表
     *
     * 【What】
     * 批量导入门店商品，支持新增和更新两种模式：
     * - 新增模式：storeId+skuId+归属 均不存在时创建
     * - 更新模式：存在时更新
     *
     * 【Constraints】
     * - 必须使用 @Transactional(rollbackFor = Exception.class)
     * - 返回结果包含成功/失败列表
     *
     * 【Pitfalls】
     * - 【陷阱】只校验了入店归属的唯一性
     * - 【陷阱】forEach 内 try-catch 不会自动回滚事务
     *
     * @param importList 导入列表
     * @param isUpdateSupport 是否支持更新
     * @return 导入结果
     */
    StoreProductImportRespVO importStoreProductList(List<StoreProductImportExcelVO> importList, boolean isUpdateSupport);

    /**
     * 获得SKU简单列表
     *
     * 【What】
     * 返回所有 SKU 的简化信息
     *
     * @return SKU简单列表
     */
    List<SkuSimpleRespVO> getSkuSimpleList();

};
