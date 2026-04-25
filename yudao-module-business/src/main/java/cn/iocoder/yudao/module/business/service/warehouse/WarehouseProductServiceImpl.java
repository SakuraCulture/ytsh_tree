package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.SkuSimpleRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSimpleRespVO;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_DELETE_HAS_STOCK;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_SKU_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_SKU_NOT_EXISTS;

@Service
@Validated
public class WarehouseProductServiceImpl implements WarehouseProductService {

    @Resource
    private WarehouseProductMapper warehouseProductMapper;
    @Resource
    private WarehouseStockMapper warehouseStockMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private SkuTableMapper skuTableMapper;

    @Override
    public Long createWarehouseProduct(WarehouseProductSaveReqVO createReqVO) {
        validateWarehouseExists(createReqVO.getWarehouseId());
        validateSkuExists(createReqVO.getProductSkuId());
        validateWarehouseProductUnique(null, createReqVO.getWarehouseId(), createReqVO.getProductSkuId());
        WarehouseProductDO warehouseProduct = BeanUtils.toBean(createReqVO, WarehouseProductDO.class);
        warehouseProductMapper.insert(warehouseProduct);
        return warehouseProduct.getWarehouseProductId();
    }

    @Override
    public void updateWarehouseProduct(WarehouseProductSaveReqVO updateReqVO) {
        validateWarehouseProductExists(updateReqVO.getWarehouseProductId());
        validateWarehouseExists(updateReqVO.getWarehouseId());
        validateSkuExists(updateReqVO.getProductSkuId());
        validateWarehouseProductUnique(updateReqVO.getWarehouseProductId(), updateReqVO.getWarehouseId(), updateReqVO.getProductSkuId());
        WarehouseProductDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseProductDO.class);
        warehouseProductMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseProduct(Long warehouseProductId) {
        validateWarehouseProductExists(warehouseProductId);
        WarehouseStockDO stock = warehouseStockMapper.selectByWarehouseProductId(warehouseProductId);
        if (stock != null && stock.getWarehouseStockQty() != null && stock.getWarehouseStockQty() > 0) {
            throw exception(WAREHOUSE_PRODUCT_DELETE_HAS_STOCK);
        }
        if (stock != null) {
            warehouseStockMapper.deleteById(stock.getWarehouseStockId());
        }
        warehouseProductMapper.deleteById(warehouseProductId);
    }

    @Override
    public WarehouseProductRespVO getWarehouseProduct(Long warehouseProductId) {
        WarehouseProductDO warehouseProduct = warehouseProductMapper.selectById(warehouseProductId);
        if (warehouseProduct == null) {
            return null;
        }
        return buildRespVO(warehouseProduct,
                getWarehouseMap(Collections.singleton(warehouseProduct.getWarehouseId())),
                getSkuMap(Collections.singleton(warehouseProduct.getProductSkuId())));
    }

    @Override
    public PageResult<WarehouseProductRespVO> getWarehouseProductPage(WarehouseProductPageReqVO pageReqVO) {
        List<Long> productSkuIds = getProductSkuIds(pageReqVO);
        if (hasSkuFilter(pageReqVO) && CollUtil.isEmpty(productSkuIds)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<WarehouseProductDO> pageResult = warehouseProductMapper.selectPage(pageReqVO, productSkuIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        Map<String, WarehouseDO> warehouseMap = getWarehouseMap(pageResult.getList().stream()
                .map(WarehouseProductDO::getWarehouseId)
                .collect(Collectors.toSet()));
        Map<Long, SkuTableDO> skuMap = getSkuMap(pageResult.getList().stream()
                .map(WarehouseProductDO::getProductSkuId)
                .collect(Collectors.toSet()));
        List<WarehouseProductRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, warehouseMap, skuMap))
                .collect(Collectors.toList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<WarehouseProductSimpleRespVO> getWarehouseProductSimpleList(String warehouseId) {
        List<WarehouseProductDO> list = warehouseProductMapper.selectListByWarehouseId(warehouseId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SkuTableDO> skuMap = getSkuMap(list.stream().map(WarehouseProductDO::getProductSkuId).collect(Collectors.toSet()));
        return list.stream().map(item -> {
            WarehouseProductSimpleRespVO respVO = new WarehouseProductSimpleRespVO();
            respVO.setWarehouseProductId(item.getWarehouseProductId());
            respVO.setWarehouseId(item.getWarehouseId());
            respVO.setProductSkuId(item.getProductSkuId());
            SkuTableDO sku = skuMap.get(item.getProductSkuId());
            if (sku != null) {
                respVO.setSkuName(sku.getProductSkuName());
            }
            return respVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SkuSimpleRespVO> getSkuSimpleList() {
        List<SkuTableDO> list = skuTableMapper.selectAllSimpleList();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, SkuSimpleRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseProductImportRespVO importWarehouseProductList(List<WarehouseProductImportExcelVO> importList, boolean updateSupport) {
        if (CollUtil.isEmpty(importList)) {
            throw exception(WAREHOUSE_PRODUCT_NOT_EXISTS);
        }

        WarehouseProductImportRespVO respVO = WarehouseProductImportRespVO.builder()
                .createCount(0)
                .updateCount(0)
                .failureCount(0)
                .failureWarehouseProductIds(new LinkedHashMap<>())
                .build();

        importList.forEach(importItem -> {
            try {
                if (StrUtil.isBlank(importItem.getWarehouseName()) || StrUtil.isBlank(importItem.getProductSkuCode())) {
                    respVO.getFailureWarehouseProductIds().put(
                            "第 " + (importList.indexOf(importItem) + 1) + " 行",
                            "仓库名称和SKU编码不能为空");
                    respVO.setFailureCount(respVO.getFailureCount() + 1);
                    return;
                }

                WarehouseDO warehouse = warehouseMapper.selectByWarehouseName(importItem.getWarehouseName());
                if (warehouse == null) {
                    respVO.getFailureWarehouseProductIds().put(
                            importItem.getWarehouseName(),
                            "仓库不存在");
                    respVO.setFailureCount(respVO.getFailureCount() + 1);
                    return;
                }

                SkuTableDO sku = skuTableMapper.selectByProductSkuCode(importItem.getProductSkuCode());
                if (sku == null) {
                    respVO.getFailureWarehouseProductIds().put(
                            importItem.getProductSkuCode(),
                            "SKU编码不存在");
                    respVO.setFailureCount(respVO.getFailureCount() + 1);
                    return;
                }

                WarehouseProductDO existProduct = warehouseProductMapper.selectByWarehouseIdAndProductSkuId(
                        warehouse.getWarehouseId(), sku.getProductSkuId());

                if (existProduct == null) {
                    WarehouseProductDO warehouseProduct = new WarehouseProductDO();
                    warehouseProduct.setWarehouseId(warehouse.getWarehouseId());
                    warehouseProduct.setProductSkuId(sku.getProductSkuId());
                    if (importItem.getWarehouseProductCostPrice() != null) {
                        warehouseProduct.setWarehouseProductCostPrice(importItem.getWarehouseProductCostPrice());
                    }
                    warehouseProduct.setWarehouseProductLocation(importItem.getWarehouseProductLocation());
                    if (importItem.getWarehouseProductFirstDate() != null) {
                        warehouseProduct.setWarehouseProductFirstDate(parseDate(importItem.getWarehouseProductFirstDate()));
                    }
                    if (importItem.getWarehouseProductLastDate() != null) {
                        warehouseProduct.setWarehouseProductLastDate(parseDate(importItem.getWarehouseProductLastDate()));
                    }
                    warehouseProductMapper.insert(warehouseProduct);
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                } else if (updateSupport) {
                    if (importItem.getWarehouseProductCostPrice() != null) {
                        existProduct.setWarehouseProductCostPrice(importItem.getWarehouseProductCostPrice());
                    }
                    if (importItem.getWarehouseProductLocation() != null) {
                        existProduct.setWarehouseProductLocation(importItem.getWarehouseProductLocation());
                    }
                    if (importItem.getWarehouseProductFirstDate() != null) {
                        existProduct.setWarehouseProductFirstDate(parseDate(importItem.getWarehouseProductFirstDate()));
                    }
                    if (importItem.getWarehouseProductLastDate() != null) {
                        existProduct.setWarehouseProductLastDate(parseDate(importItem.getWarehouseProductLastDate()));
                    }
                    warehouseProductMapper.updateById(existProduct);
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                } else {
                    respVO.getFailureWarehouseProductIds().put(
                            importItem.getWarehouseName() + "_" + importItem.getProductSkuCode(),
                            "仓库商品已存在，不允许重复导入");
                    respVO.setFailureCount(respVO.getFailureCount() + 1);
                }
            } catch (Exception ex) {
                String key = (importItem.getWarehouseName() != null && !importItem.getWarehouseName().isEmpty())
                        ? importItem.getWarehouseName() + "_" + importItem.getProductSkuCode()
                        : "第 " + (importList.indexOf(importItem) + 1) + " 行";
                respVO.getFailureWarehouseProductIds().put(key, ex.getMessage());
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        });

        return respVO;
    }

    private WarehouseProductDO validateWarehouseProductExists(Long warehouseProductId) {
        WarehouseProductDO warehouseProduct = warehouseProductMapper.selectById(warehouseProductId);
        if (warehouseProduct == null) {
            throw exception(WAREHOUSE_PRODUCT_NOT_EXISTS);
        }
        return warehouseProduct;
    }

    private void validateWarehouseExists(String warehouseId) {
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
    }

    private void validateSkuExists(Long productSkuId) {
        if (skuTableMapper.selectById(productSkuId) == null) {
            throw exception(WAREHOUSE_PRODUCT_SKU_NOT_EXISTS);
        }
    }

    private void validateWarehouseProductUnique(Long warehouseProductId, String warehouseId, Long productSkuId) {
        WarehouseProductDO warehouseProduct = warehouseProductMapper.selectByWarehouseIdAndProductSkuId(warehouseId, productSkuId);
        if (warehouseProduct != null && !warehouseProduct.getWarehouseProductId().equals(warehouseProductId)) {
            throw exception(WAREHOUSE_PRODUCT_SKU_EXISTS);
        }
    }

    private boolean hasSkuFilter(WarehouseProductPageReqVO pageReqVO) {
        return StrUtil.isNotBlank(pageReqVO.getSkuCode()) || StrUtil.isNotBlank(pageReqVO.getSkuName());
    }

    private List<Long> getProductSkuIds(WarehouseProductPageReqVO pageReqVO) {
        if (pageReqVO.getProductSkuId() != null) {
            return Collections.singletonList(pageReqVO.getProductSkuId());
        }
        if (!hasSkuFilter(pageReqVO)) {
            return null;
        }
        List<SkuTableDO> skuList = skuTableMapper.selectListByKeyword(pageReqVO.getSkuCode(), pageReqVO.getSkuName());
        if (CollUtil.isEmpty(skuList)) {
            return Collections.emptyList();
        }
        return skuList.stream().map(SkuTableDO::getProductSkuId).filter(Objects::nonNull).collect(Collectors.toList());
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

    private WarehouseProductRespVO buildRespVO(WarehouseProductDO warehouseProduct,
                                               Map<String, WarehouseDO> warehouseMap,
                                               Map<Long, SkuTableDO> skuMap) {
        WarehouseProductRespVO respVO = new WarehouseProductRespVO();
        respVO.setWarehouseProductId(warehouseProduct.getWarehouseProductId());
        respVO.setWarehouseId(warehouseProduct.getWarehouseId());
        respVO.setProductSkuId(warehouseProduct.getProductSkuId());
        respVO.setWarehouseProductCostPrice(warehouseProduct.getWarehouseProductCostPrice());
        respVO.setWarehouseProductLocation(warehouseProduct.getWarehouseProductLocation());
        respVO.setWarehouseProductFirstDate(warehouseProduct.getWarehouseProductFirstDate());
        respVO.setWarehouseProductLastDate(warehouseProduct.getWarehouseProductLastDate());
        respVO.setCreateTime(warehouseProduct.getCreateTime());

        WarehouseDO warehouse = warehouseMap.get(warehouseProduct.getWarehouseId());
        if (warehouse != null) {
            respVO.setWarehouseName(warehouse.getWarehouseName());
        }
        SkuTableDO sku = skuMap.get(warehouseProduct.getProductSkuId());
        if (sku != null) {
            respVO.setSkuCode(sku.getProductSkuCode());
            respVO.setSkuName(sku.getProductSkuName());
            respVO.setRetailPrice(sku.getProductRetailPrice());
        }
        return respVO;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e1) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/M/d"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                } catch (Exception e3) {
                    throw new IllegalArgumentException("日期格式错误: " + dateStr + "，支持格式：yyyy-MM-dd, yyyy/M/d, yyyy/MM/dd");
                }
            }
        }
    }

}
