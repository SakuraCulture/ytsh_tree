package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockStatisticsRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class WarehouseStockServiceImpl implements WarehouseStockService {

    @Resource
    private WarehouseStockMapper warehouseStockMapper;
    @Resource
    private WarehouseProductMapper warehouseProductMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private SkuTableMapper skuTableMapper;

    @Override
    public PageResult<WarehouseStockRespVO> getWarehouseStockPage(WarehouseStockPageReqVO pageReqVO) {
        Collection<Long> warehouseProductIds = getWarehouseProductIds(pageReqVO);
        if (isFilterByWarehouseProduct(pageReqVO) && CollUtil.isEmpty(warehouseProductIds)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<WarehouseStockDO> pageResult = warehouseStockMapper.selectPage(pageReqVO, warehouseProductIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return buildStockPageResult(pageResult);
    }

    @Override
    public WarehouseStockRespVO getWarehouseStock(Long warehouseStockId) {
        WarehouseStockDO warehouseStock = warehouseStockMapper.selectById(warehouseStockId);
        if (warehouseStock == null) {
            return null;
        }
        Map<Long, WarehouseProductDO> warehouseProductMap = getWarehouseProductMap(Collections.singleton(warehouseStock.getWarehouseProductId()));
        Map<String, WarehouseDO> warehouseMap = getWarehouseMap(warehouseProductMap.values().stream()
                .map(WarehouseProductDO::getWarehouseId)
                .collect(Collectors.toSet()));
        Map<Long, SkuTableDO> skuMap = getSkuMap(warehouseProductMap.values().stream()
                .map(WarehouseProductDO::getProductSkuId)
                .collect(Collectors.toSet()));
        return buildRespVO(warehouseStock, warehouseProductMap, warehouseMap, skuMap);
    }

    @Override
    public WarehouseStockStatisticsRespVO getWarehouseStockStatistics(WarehouseStockPageReqVO pageReqVO) {
        Collection<Long> warehouseProductIds = getWarehouseProductIds(pageReqVO);
        if (isFilterByWarehouseProduct(pageReqVO) && CollUtil.isEmpty(warehouseProductIds)) {
            return WarehouseStockStatisticsRespVO.builder()
                    .stockCount(0L)
                    .totalQuantity(0)
                    .totalAvailableQuantity(0)
                    .totalTransitQuantity(0)
                    .totalFrozenQuantity(0)
                    .build();
        }
        List<WarehouseStockDO> list;
        if (CollUtil.isEmpty(warehouseProductIds)) {
            list = warehouseStockMapper.selectList(new LambdaQueryWrapperX<WarehouseStockDO>());
        } else {
            list = warehouseStockMapper.selectListByWarehouseProductIds(warehouseProductIds);
        }
        return WarehouseStockStatisticsRespVO.builder()
                .stockCount((long) list.size())
                .totalQuantity(sum(list.stream().map(WarehouseStockDO::getWarehouseStockQty).collect(Collectors.toList())))
                .totalAvailableQuantity(sum(list.stream().map(WarehouseStockDO::getWarehouseStockAvailableQty).collect(Collectors.toList())))
                .totalTransitQuantity(sum(list.stream().map(WarehouseStockDO::getWarehouseStockTransitQty).collect(Collectors.toList())))
                .totalFrozenQuantity(sum(list.stream().map(WarehouseStockDO::getWarehouseStockFrozenQty).collect(Collectors.toList())))
                .build();
    }

    private PageResult<WarehouseStockRespVO> buildStockPageResult(PageResult<WarehouseStockDO> pageResult) {
        Map<Long, WarehouseProductDO> warehouseProductMap = getWarehouseProductMap(pageResult.getList().stream()
                .map(WarehouseStockDO::getWarehouseProductId)
                .collect(Collectors.toSet()));
        Map<String, WarehouseDO> warehouseMap = getWarehouseMap(warehouseProductMap.values().stream()
                .map(WarehouseProductDO::getWarehouseId)
                .collect(Collectors.toSet()));
        Map<Long, SkuTableDO> skuMap = getSkuMap(warehouseProductMap.values().stream()
                .map(WarehouseProductDO::getProductSkuId)
                .collect(Collectors.toSet()));
        List<WarehouseStockRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, warehouseProductMap, warehouseMap, skuMap))
                .collect(Collectors.toList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    private boolean isFilterByWarehouseProduct(WarehouseStockPageReqVO pageReqVO) {
        return pageReqVO.getWarehouseProductId() != null
                || StrUtil.isNotBlank(pageReqVO.getWarehouseId())
                || pageReqVO.getProductSkuId() != null
                || StrUtil.isNotBlank(pageReqVO.getSkuCode())
                || StrUtil.isNotBlank(pageReqVO.getSkuName());
    }

    private Collection<Long> getWarehouseProductIds(WarehouseStockPageReqVO pageReqVO) {
        if (pageReqVO.getWarehouseProductId() != null) {
            return Collections.singleton(pageReqVO.getWarehouseProductId());
        }
        if (!isFilterByWarehouseProduct(pageReqVO)) {
            return null;
        }
        List<Long> productSkuIds = getProductSkuIds(pageReqVO);
        if ((pageReqVO.getProductSkuId() != null || StrUtil.isNotBlank(pageReqVO.getSkuCode()) || StrUtil.isNotBlank(pageReqVO.getSkuName()))
                && CollUtil.isEmpty(productSkuIds)) {
            return Collections.emptyList();
        }
        List<WarehouseProductDO> list = warehouseProductMapper.selectList(new LambdaQueryWrapperX<WarehouseProductDO>()
                .eqIfPresent(WarehouseProductDO::getWarehouseId, pageReqVO.getWarehouseId())
                .inIfPresent(WarehouseProductDO::getProductSkuId, productSkuIds));
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(WarehouseProductDO::getWarehouseProductId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> getProductSkuIds(WarehouseStockPageReqVO pageReqVO) {
        if (pageReqVO.getProductSkuId() != null) {
            return Collections.singletonList(pageReqVO.getProductSkuId());
        }
        if (StrUtil.isBlank(pageReqVO.getSkuCode()) && StrUtil.isBlank(pageReqVO.getSkuName())) {
            return null;
        }
        List<SkuTableDO> skuList = skuTableMapper.selectListByKeyword(pageReqVO.getSkuCode(), pageReqVO.getSkuName());
        if (CollUtil.isEmpty(skuList)) {
            return Collections.emptyList();
        }
        return skuList.stream().map(SkuTableDO::getProductSkuId).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<Long, WarehouseProductDO> getWarehouseProductMap(Collection<Long> warehouseProductIds) {
        if (CollUtil.isEmpty(warehouseProductIds)) {
            return Collections.emptyMap();
        }
        return warehouseProductMapper.selectListByWarehouseProductIds(warehouseProductIds).stream()
                .collect(Collectors.toMap(WarehouseProductDO::getWarehouseProductId, Function.identity(), (item1, item2) -> item1));
    }

    private Map<String, WarehouseDO> getWarehouseMap(Collection<String> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectListByWarehouseIds(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseDO::getWarehouseId, Function.identity(), (item1, item2) -> item1));
    }

    private Map<Long, SkuTableDO> getSkuMap(Collection<Long> productSkuIds) {
        if (CollUtil.isEmpty(productSkuIds)) {
            return Collections.emptyMap();
        }
        return skuTableMapper.selectList(new LambdaQueryWrapperX<SkuTableDO>()
                        .inIfPresent(SkuTableDO::getProductSkuId, productSkuIds))
                .stream()
                .collect(Collectors.toMap(SkuTableDO::getProductSkuId, Function.identity(), (item1, item2) -> item1));
    }

    private WarehouseStockRespVO buildRespVO(WarehouseStockDO warehouseStock,
                                             Map<Long, WarehouseProductDO> warehouseProductMap,
                                             Map<String, WarehouseDO> warehouseMap,
                                             Map<Long, SkuTableDO> skuMap) {
        WarehouseStockRespVO respVO = new WarehouseStockRespVO();
        respVO.setWarehouseStockId(warehouseStock.getWarehouseStockId());
        respVO.setWarehouseProductId(warehouseStock.getWarehouseProductId());
        respVO.setWarehouseStockQty(warehouseStock.getWarehouseStockQty());
        respVO.setWarehouseStockAvailableQty(warehouseStock.getWarehouseStockAvailableQty());
        respVO.setWarehouseStockTransitQty(warehouseStock.getWarehouseStockTransitQty());
        respVO.setWarehouseStockFrozenQty(warehouseStock.getWarehouseStockFrozenQty());
        respVO.setWarehouseStockOutstockHours(warehouseStock.getWarehouseStockOutstockHours());
        respVO.setCreateTime(warehouseStock.getCreateTime());

        WarehouseProductDO warehouseProduct = warehouseProductMap.get(warehouseStock.getWarehouseProductId());
        if (warehouseProduct != null) {
            respVO.setWarehouseId(warehouseProduct.getWarehouseId());
            respVO.setProductSkuId(warehouseProduct.getProductSkuId());
            WarehouseDO warehouse = warehouseMap.get(warehouseProduct.getWarehouseId());
            if (warehouse != null) {
                respVO.setWarehouseName(warehouse.getWarehouseName());
            }
            SkuTableDO sku = skuMap.get(warehouseProduct.getProductSkuId());
            if (sku != null) {
                respVO.setSkuCode(sku.getProductSkuCode());
                respVO.setSkuName(sku.getProductSkuName());
            }
        }
        return respVO;
    }

    private Integer sum(List<Integer> values) {
        return values.stream().filter(Objects::nonNull).reduce(0, Integer::sum);
    }

}
