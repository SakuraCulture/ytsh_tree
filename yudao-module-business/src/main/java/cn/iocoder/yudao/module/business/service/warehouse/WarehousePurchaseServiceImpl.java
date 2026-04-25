package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchasePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehousePurchaseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehousePurchaseDetailDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseSupplierDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehousePurchaseDetailMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehousePurchaseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStockMapper;
import cn.iocoder.yudao.module.business.dal.redis.no.WarehouseNoRedisDAO;
import cn.iocoder.yudao.module.business.enums.warehouse.WarehousePurchaseReceiveStatusEnum;
import cn.iocoder.yudao.module.business.enums.warehouse.WarehousePurchaseStatusEnum;
import cn.iocoder.yudao.module.business.enums.warehouse.WarehouseStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.business.service.warehouse.bo.WarehouseStockRecordCreateReqBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PRODUCT_SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_AUDIT_FAIL;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_CANCEL_FAIL;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_DELETE_FAIL;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_DETAIL_EMPTY;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_INBOUND_FAIL;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_ORDER_NO_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_UPDATE_FAIL;

@Service
@Validated
public class WarehousePurchaseServiceImpl implements WarehousePurchaseService {

    @Resource
    private WarehousePurchaseMapper warehousePurchaseMapper;
    @Resource
    private WarehousePurchaseDetailMapper warehousePurchaseDetailMapper;
    @Resource
    private WarehouseProductMapper warehouseProductMapper;
    @Resource
    private WarehouseStockMapper warehouseStockMapper;
    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private WarehouseNoRedisDAO warehouseNoRedisDAO;
    @Resource
    private WarehouseService warehouseService;
    @Resource
    private WarehouseSupplierService warehouseSupplierService;
    @Resource
    private WarehouseStockRecordService warehouseStockRecordService;
    @Resource
    private WarehouseProductService warehouseProductService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehousePurchase(WarehousePurchaseSaveReqVO createReqVO) {
        WarehouseSupplierDO supplier = warehouseSupplierService.validateWarehouseSupplierExists(createReqVO.getSupplierId());
        warehouseService.validateWarehouseExists(createReqVO.getWarehouseId());
        String purchaseOrderNo = warehouseNoRedisDAO.generate(WarehouseNoRedisDAO.PURCHASE_ORDER_NO_PREFIX);
        if (warehousePurchaseMapper.selectByPurchaseOrderNo(purchaseOrderNo) != null) {
            throw exception(WAREHOUSE_PURCHASE_ORDER_NO_EXISTS);
        }
        List<WarehousePurchaseDetailDO> detailList = buildPurchaseDetailList(createReqVO.getWarehouseId(), purchaseOrderNo, createReqVO.getItems());
        WarehousePurchaseDO purchase = BeanUtils.toBean(createReqVO, WarehousePurchaseDO.class, bean -> {
            bean.setPurchaseOrderNo(purchaseOrderNo);
            bean.setSupplierName(supplier.getSupplierName());
            bean.setOrderStatus(WarehousePurchaseStatusEnum.DRAFT.getStatus());
            bean.setReceiveStatus(WarehousePurchaseReceiveStatusEnum.PENDING_RECEIVE.getStatus());
            bean.setTotalInboundQty(0);
            bean.setDiffQty(0);
            bean.setReturnQty(0);
        });
        fillPurchaseSummary(purchase, detailList);
        warehousePurchaseMapper.insert(purchase);
        detailList.forEach(item -> item.setPurchaseOrderId(purchase.getPurchaseOrderId()));
        warehousePurchaseDetailMapper.insertBatch(detailList);
        return purchase.getPurchaseOrderId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehousePurchase(WarehousePurchaseSaveReqVO updateReqVO) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(updateReqVO.getPurchaseOrderId());
        validateCanUpdate(purchase);
        WarehouseSupplierDO supplier = warehouseSupplierService.validateWarehouseSupplierExists(updateReqVO.getSupplierId());
        warehouseService.validateWarehouseExists(updateReqVO.getWarehouseId());
        List<WarehousePurchaseDetailDO> detailList = buildPurchaseDetailList(updateReqVO.getWarehouseId(), purchase.getPurchaseOrderNo(), updateReqVO.getItems());
        WarehousePurchaseDO updateObj = BeanUtils.toBean(updateReqVO, WarehousePurchaseDO.class, bean -> {
            bean.setPurchaseOrderNo(purchase.getPurchaseOrderNo());
            bean.setSupplierName(supplier.getSupplierName());
            bean.setOrderStatus(purchase.getOrderStatus());
            bean.setReceiveStatus(purchase.getReceiveStatus());
            bean.setTotalInboundQty(purchase.getTotalInboundQty());
            bean.setDiffQty(purchase.getDiffQty());
            bean.setReturnQty(purchase.getReturnQty());
            bean.setAuditDate(purchase.getAuditDate());
        });
        fillPurchaseSummary(updateObj, detailList);
        warehousePurchaseMapper.updateById(updateObj);
        warehousePurchaseDetailMapper.deleteByPurchaseOrderId(purchase.getPurchaseOrderId());
        detailList.forEach(item -> item.setPurchaseOrderId(purchase.getPurchaseOrderId()));
        warehousePurchaseDetailMapper.insertBatch(detailList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehousePurchase(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(purchaseOrderId);
        if (!WarehousePurchaseStatusEnum.DRAFT.getStatus().equals(purchase.getOrderStatus())
                && !WarehousePurchaseStatusEnum.CANCELLED.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_DELETE_FAIL);
        }
        warehousePurchaseMapper.deleteById(purchaseOrderId);
        warehousePurchaseDetailMapper.deleteByPurchaseOrderId(purchaseOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitWarehousePurchase(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(purchaseOrderId);
        if (!WarehousePurchaseStatusEnum.DRAFT.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_SUBMIT_FAIL);
        }
        int updateCount = warehousePurchaseMapper.updateByIdAndOrderStatus(purchaseOrderId,
                WarehousePurchaseStatusEnum.DRAFT.getStatus(), WarehousePurchaseDO.builder()
                        .purchaseOrderId(purchaseOrderId)
                        .orderStatus(WarehousePurchaseStatusEnum.PENDING_AUDIT.getStatus())
                        .build());
        if (updateCount == 0) {
            throw exception(WAREHOUSE_PURCHASE_SUBMIT_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditWarehousePurchase(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(purchaseOrderId);
        if (!WarehousePurchaseStatusEnum.PENDING_AUDIT.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_AUDIT_FAIL);
        }
        int updateCount = warehousePurchaseMapper.updateByIdAndOrderStatus(purchaseOrderId,
                WarehousePurchaseStatusEnum.PENDING_AUDIT.getStatus(), WarehousePurchaseDO.builder()
                        .purchaseOrderId(purchaseOrderId)
                        .orderStatus(WarehousePurchaseStatusEnum.APPROVED.getStatus())
                        .auditDate(LocalDate.now())
                        .build());
        if (updateCount == 0) {
            throw exception(WAREHOUSE_PURCHASE_AUDIT_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmInbound(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(purchaseOrderId);
        if (!WarehousePurchaseStatusEnum.APPROVED.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_INBOUND_FAIL);
        }
        List<WarehousePurchaseDetailDO> detailList = warehousePurchaseDetailMapper.selectListByPurchaseOrderId(purchaseOrderId);
        LocalDate inboundDate = LocalDate.now();
        List<WarehousePurchaseDetailDO> updateDetailList = new ArrayList<>();
        for (WarehousePurchaseDetailDO detail : detailList) {
            WarehouseProductDO warehouseProduct = validateWarehouseProductExists(purchase.getWarehouseId(), detail.getProductSkuId());
            int inboundQty = defaultZero(detail.getPurchaseQty());
            WarehouseStockDO stock = warehouseStockMapper.selectByWarehouseProductId(warehouseProduct.getWarehouseProductId());
            if (stock == null) {
                stock = WarehouseStockDO.builder()
                        .warehouseProductId(warehouseProduct.getWarehouseProductId())
                        .warehouseStockQty(inboundQty)
                        .warehouseStockAvailableQty(inboundQty)
                        .warehouseStockTransitQty(0)
                        .warehouseStockFrozenQty(0)
                        .warehouseStockOutstockHours(0)
                        .build();
                warehouseStockMapper.insert(stock);
            } else {
                int totalQty = defaultZero(stock.getWarehouseStockQty()) + inboundQty;
                int availableQty = defaultZero(stock.getWarehouseStockAvailableQty()) + inboundQty;
                warehouseStockMapper.updateById(WarehouseStockDO.builder()
                        .warehouseStockId(stock.getWarehouseStockId())
                        .warehouseStockQty(totalQty)
                        .warehouseStockAvailableQty(availableQty)
                        .warehouseStockTransitQty(defaultZero(stock.getWarehouseStockTransitQty()))
                        .warehouseStockFrozenQty(defaultZero(stock.getWarehouseStockFrozenQty()))
                        .warehouseStockOutstockHours(0)
                        .build());
            }
            warehouseProductMapper.updateById(WarehouseProductDO.builder()
                    .warehouseProductId(warehouseProduct.getWarehouseProductId())
                    .warehouseProductFirstDate(warehouseProduct.getWarehouseProductFirstDate() == null ? inboundDate : warehouseProduct.getWarehouseProductFirstDate())
                    .warehouseProductLastDate(inboundDate)
                    .build());
            updateDetailList.add(WarehousePurchaseDetailDO.builder()
                    .detailId(detail.getDetailId())
                    .inboundQty(inboundQty)
                    .build());
            // warehouseStockRecordService.createStockRecord(new WarehouseStockRecordCreateReqBO(
            //         purchase.getWarehouseId(), warehouseProduct.getWarehouseProductId(), detail.getProductSkuId(), inboundQty,
            //         WarehouseStockRecordBizTypeEnum.PURCHASE_IN.getType(), purchase.getPurchaseOrderId(), detail.getDetailId(),
            //         purchase.getPurchaseOrderNo()));
        }
        if (CollUtil.isNotEmpty(updateDetailList)) {
            warehousePurchaseDetailMapper.updateBatch(updateDetailList);
        }
        int updateCount = warehousePurchaseMapper.updateByIdAndOrderStatus(purchaseOrderId,
                WarehousePurchaseStatusEnum.APPROVED.getStatus(), WarehousePurchaseDO.builder()
                        .purchaseOrderId(purchaseOrderId)
                        .orderStatus(WarehousePurchaseStatusEnum.INBOUND.getStatus())
                        .receiveStatus(WarehousePurchaseReceiveStatusEnum.RECEIVED.getStatus())
                        .totalInboundQty(defaultZero(purchase.getTotalQty()))
                        .diffQty(0)
                        .build());
        if (updateCount == 0) {
            throw exception(WAREHOUSE_PURCHASE_INBOUND_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWarehousePurchase(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = validateWarehousePurchaseExists(purchaseOrderId);
        if (WarehousePurchaseStatusEnum.INBOUND.getStatus().equals(purchase.getOrderStatus())
                || WarehousePurchaseStatusEnum.COMPLETED.getStatus().equals(purchase.getOrderStatus())
                || WarehousePurchaseStatusEnum.CANCELLED.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_CANCEL_FAIL);
        }
        int updateCount = warehousePurchaseMapper.updateByIdAndOrderStatus(purchaseOrderId, purchase.getOrderStatus(),
                WarehousePurchaseDO.builder()
                        .purchaseOrderId(purchaseOrderId)
                        .orderStatus(WarehousePurchaseStatusEnum.CANCELLED.getStatus())
                        .build());
        if (updateCount == 0) {
            throw exception(WAREHOUSE_PURCHASE_CANCEL_FAIL);
        }
    }

    @Override
    public WarehousePurchaseRespVO getWarehousePurchase(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = warehousePurchaseMapper.selectById(purchaseOrderId);
        if (purchase == null) {
            return null;
        }
        List<WarehousePurchaseRespVO.Item> items = getWarehousePurchaseDetailList(Collections.singletonList(purchaseOrderId));
        return buildRespVO(purchase, items,
                getWarehouseMap(Collections.singleton(purchase.getWarehouseId())),
                getSupplierMap(Collections.singleton(purchase.getSupplierId())));
    }

    @Override
    public PageResult<WarehousePurchaseRespVO> getWarehousePurchasePage(WarehousePurchasePageReqVO pageReqVO) {
        Collection<Long> purchaseOrderIds = getPurchaseOrderIds(pageReqVO);
        if (hasSkuFilter(pageReqVO) && CollUtil.isEmpty(purchaseOrderIds)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<WarehousePurchaseDO> pageResult = warehousePurchaseMapper.selectPage(pageReqVO, purchaseOrderIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<Long> purchaseIds = pageResult.getList().stream().map(WarehousePurchaseDO::getPurchaseOrderId).collect(Collectors.toList());
        List<WarehousePurchaseRespVO.Item> itemList = getWarehousePurchaseDetailList(purchaseIds);
        Map<Long, List<WarehousePurchaseRespVO.Item>> itemMap = itemList.stream()
                .collect(Collectors.groupingBy(WarehousePurchaseRespVO.Item::getPurchaseOrderId));
        Map<String, WarehouseDO> warehouseMap = getWarehouseMap(pageResult.getList().stream()
                .map(WarehousePurchaseDO::getWarehouseId)
                .collect(Collectors.toSet()));
        Map<String, WarehouseSupplierDO> supplierMap = getSupplierMap(pageResult.getList().stream()
                .map(WarehousePurchaseDO::getSupplierId)
                .collect(Collectors.toSet()));
        return BeanUtils.toBean(pageResult, WarehousePurchaseRespVO.class, item -> {
            item.setItems(itemMap.getOrDefault(item.getPurchaseOrderId(), Collections.emptyList()));
            item.setProductNames(CollUtil.join(item.getItems(), "，", WarehousePurchaseRespVO.Item::getProductSkuName));
            WarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                item.setWarehouseName(warehouse.getWarehouseName());
            }
            WarehouseSupplierDO supplier = supplierMap.get(item.getSupplierId());
            if (supplier != null && StrUtil.isBlank(item.getSupplierName())) {
                item.setSupplierName(supplier.getSupplierName());
            }
        });
    }

    @Override
    public List<WarehousePurchaseRespVO.Item> getWarehousePurchaseDetailList(List<Long> purchaseOrderIds) {
        if (CollUtil.isEmpty(purchaseOrderIds)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(warehousePurchaseDetailMapper.selectListByPurchaseOrderIds(purchaseOrderIds),
                WarehousePurchaseRespVO.Item.class);
    }

    private WarehousePurchaseRespVO buildRespVO(WarehousePurchaseDO purchase,
                                                List<WarehousePurchaseRespVO.Item> items,
                                                Map<String, WarehouseDO> warehouseMap,
                                                Map<String, WarehouseSupplierDO> supplierMap) {
        WarehousePurchaseRespVO respVO = BeanUtils.toBean(purchase, WarehousePurchaseRespVO.class);
        respVO.setItems(items);
        respVO.setProductNames(CollUtil.join(items, "，", WarehousePurchaseRespVO.Item::getProductSkuName));
        WarehouseDO warehouse = warehouseMap.get(purchase.getWarehouseId());
        if (warehouse != null) {
            respVO.setWarehouseName(warehouse.getWarehouseName());
        }
        WarehouseSupplierDO supplier = supplierMap.get(purchase.getSupplierId());
        if (supplier != null && StrUtil.isBlank(respVO.getSupplierName())) {
            respVO.setSupplierName(supplier.getSupplierName());
        }
        return respVO;
    }

    private Collection<Long> getPurchaseOrderIds(WarehousePurchasePageReqVO pageReqVO) {
        if (pageReqVO.getPurchaseOrderId() != null) {
            return Collections.singleton(pageReqVO.getPurchaseOrderId());
        }
        if (!hasSkuFilter(pageReqVO)) {
            return null;
        }
        Collection<Long> productSkuIds = getProductSkuIds(pageReqVO);
        if (CollUtil.isEmpty(productSkuIds)) {
            return Collections.emptyList();
        }
        List<WarehousePurchaseDetailDO> detailList = warehousePurchaseDetailMapper.selectList(new LambdaQueryWrapperX<WarehousePurchaseDetailDO>()
                .inIfPresent(WarehousePurchaseDetailDO::getProductSkuId, productSkuIds));
        if (CollUtil.isEmpty(detailList)) {
            return Collections.emptyList();
        }
        return detailList.stream().map(WarehousePurchaseDetailDO::getPurchaseOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean hasSkuFilter(WarehousePurchasePageReqVO pageReqVO) {
        return pageReqVO.getProductSkuId() != null
                || StrUtil.isNotBlank(pageReqVO.getSkuCode())
                || StrUtil.isNotBlank(pageReqVO.getSkuName());
    }

    private Collection<Long> getProductSkuIds(WarehousePurchasePageReqVO pageReqVO) {
        if (pageReqVO.getProductSkuId() != null) {
            return Collections.singleton(pageReqVO.getProductSkuId());
        }
        if (!hasSkuFilter(pageReqVO)) {
            return null;
        }
        return skuTableMapper.selectListByKeyword(pageReqVO.getSkuCode(), pageReqVO.getSkuName()).stream()
                .map(SkuTableDO::getProductSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<WarehousePurchaseDetailDO> buildPurchaseDetailList(String warehouseId, String purchaseOrderNo,
                                                                    List<WarehousePurchaseSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_PURCHASE_DETAIL_EMPTY);
        }
        Set<Long> productSkuIds = items.stream().map(WarehousePurchaseSaveReqVO.Item::getProductSkuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SkuTableDO> skuMap = skuTableMapper.selectList(new LambdaQueryWrapperX<SkuTableDO>()
                        .inIfPresent(SkuTableDO::getProductSkuId, productSkuIds))
                .stream()
                .collect(Collectors.toMap(SkuTableDO::getProductSkuId, Function.identity(), (item1, item2) -> item1));
        List<WarehousePurchaseDetailDO> detailList = new ArrayList<>(items.size());
        for (WarehousePurchaseSaveReqVO.Item item : items) {
            SkuTableDO sku = skuMap.get(item.getProductSkuId());
            if (sku == null) {
                throw exception(WAREHOUSE_PRODUCT_SKU_NOT_EXISTS);
            }
            WarehouseProductDO warehouseProduct = getOrCreateWarehouseProduct(warehouseId, item.getProductSkuId());
            BigDecimal purchasePrice = item.getPurchasePrice();
            if (purchasePrice == null) {
                purchasePrice = warehouseProduct.getWarehouseProductCostPrice();
            }
            if (purchasePrice == null) {
                purchasePrice = sku.getProductCostPrice();
            }
            if (purchasePrice == null) {
                purchasePrice = BigDecimal.ZERO;
            }
            BigDecimal finalPurchasePrice = purchasePrice;
            WarehousePurchaseDetailDO detail = BeanUtils.toBean(item, WarehousePurchaseDetailDO.class, bean -> {
                bean.setDetailId(null);
                bean.setPurchaseOrderNo(purchaseOrderNo);
                bean.setProductSkuCode(sku.getProductSkuCode());
                bean.setProductSkuName(sku.getProductSkuName());
                bean.setPurchasePrice(finalPurchasePrice);
                bean.setPurchaseAmount(finalPurchasePrice.multiply(BigDecimal.valueOf(defaultZero(item.getPurchaseQty()))));
                bean.setInboundQty(0);
                bean.setReturnQty(0);
                bean.setDiffQty(0);
            });
            detailList.add(detail);
        }
        return detailList;
    }

    private void fillPurchaseSummary(WarehousePurchaseDO purchase, List<WarehousePurchaseDetailDO> detailList) {
        purchase.setTotalQty(detailList.stream().map(WarehousePurchaseDetailDO::getPurchaseQty)
                .filter(Objects::nonNull).reduce(0, Integer::sum));
        purchase.setTotalAmount(detailList.stream().map(WarehousePurchaseDetailDO::getPurchaseAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void validateCanUpdate(WarehousePurchaseDO purchase) {
        if (!WarehousePurchaseStatusEnum.DRAFT.getStatus().equals(purchase.getOrderStatus())) {
            throw exception(WAREHOUSE_PURCHASE_UPDATE_FAIL);
        }
    }

    private WarehousePurchaseDO validateWarehousePurchaseExists(Long purchaseOrderId) {
        WarehousePurchaseDO purchase = warehousePurchaseMapper.selectById(purchaseOrderId);
        if (purchase == null) {
            throw exception(WAREHOUSE_PURCHASE_NOT_EXISTS);
        }
        return purchase;
    }

    private WarehouseProductDO validateWarehouseProductExists(String warehouseId, Long productSkuId) {
        WarehouseProductDO warehouseProduct = warehouseProductMapper.selectByWarehouseIdAndProductSkuId(warehouseId, productSkuId);
        if (warehouseProduct == null) {
            throw exception(WAREHOUSE_PRODUCT_NOT_EXISTS);
        }
        return warehouseProduct;
    }

    private WarehouseProductDO getOrCreateWarehouseProduct(String warehouseId, Long productSkuId) {
        WarehouseProductDO warehouseProduct = warehouseProductMapper.selectByWarehouseIdAndProductSkuId(warehouseId, productSkuId);
        if (warehouseProduct != null) {
            return warehouseProduct;
        }
        WarehouseProductSaveReqVO createReqVO = new WarehouseProductSaveReqVO();
        createReqVO.setWarehouseId(warehouseId);
        createReqVO.setProductSkuId(productSkuId);
        Long warehouseProductId = warehouseProductService.createWarehouseProduct(createReqVO);
        warehouseProduct = warehouseProductMapper.selectById(warehouseProductId);
        return warehouseProduct;
    }

    private Map<String, WarehouseDO> getWarehouseMap(Collection<String> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return warehouseService.getWarehouseList(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseDO::getWarehouseId, Function.identity(), (item1, item2) -> item1));
    }

    private Map<String, WarehouseSupplierDO> getSupplierMap(Collection<String> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return Collections.emptyMap();
        }
        return warehouseSupplierService.getWarehouseSupplierList(supplierIds).stream()
                .collect(Collectors.toMap(WarehouseSupplierDO::getSupplierId, Function.identity(), (item1, item2) -> item1));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

}
