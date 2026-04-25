package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockRecordDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStockMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStockRecordMapper;
import cn.iocoder.yudao.module.business.service.warehouse.bo.WarehouseStockRecordCreateReqBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class WarehouseStockRecordServiceImpl implements WarehouseStockRecordService {

    @Resource
    private WarehouseStockRecordMapper warehouseStockRecordMapper;
    @Resource
    private WarehouseStockMapper warehouseStockMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private SkuTableMapper skuTableMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStockRecord(WarehouseStockRecordCreateReqBO createReqBO) {
        WarehouseStockDO stock = warehouseStockMapper.selectByWarehouseProductId(createReqBO.getWarehouseProductId());
        Integer afterQty = stock != null && stock.getWarehouseStockQty() != null ? stock.getWarehouseStockQty() : 0;
        WarehouseStockRecordDO stockRecord = BeanUtils.toBean(createReqBO, WarehouseStockRecordDO.class, bean -> bean.setAfterQty(afterQty));
        warehouseStockRecordMapper.insert(stockRecord);
    }

    @Override
    public PageResult<WarehouseStockRecordRespVO> getWarehouseStockRecordPage(WarehouseStockRecordPageReqVO pageReqVO) {
        Collection<Long> productSkuIds = getProductSkuIds(pageReqVO);
        if (hasSkuFilter(pageReqVO) && CollUtil.isEmpty(productSkuIds)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<WarehouseStockRecordDO> pageResult = warehouseStockRecordMapper.selectPage(pageReqVO, productSkuIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return buildPageResult(pageResult);
    }

    @Override
    public WarehouseStockRecordRespVO getWarehouseStockRecord(Long stockRecordId) {
        WarehouseStockRecordDO stockRecord = warehouseStockRecordMapper.selectById(stockRecordId);
        if (stockRecord == null) {
            return null;
        }
        return buildRespVO(stockRecord,
                getWarehouseMap(Collections.singleton(stockRecord.getWarehouseId())),
                getSkuMap(Collections.singleton(stockRecord.getProductSkuId())));
    }

    private boolean hasSkuFilter(WarehouseStockRecordPageReqVO pageReqVO) {
        return pageReqVO.getProductSkuId() != null
                || StrUtil.isNotBlank(pageReqVO.getSkuCode())
                || StrUtil.isNotBlank(pageReqVO.getSkuName());
    }

    private Collection<Long> getProductSkuIds(WarehouseStockRecordPageReqVO pageReqVO) {
        if (pageReqVO.getProductSkuId() != null) {
            return Collections.singleton(pageReqVO.getProductSkuId());
        }
        if (!hasSkuFilter(pageReqVO)) {
            return null;
        }
        return skuTableMapper.selectListByKeyword(pageReqVO.getSkuCode(), pageReqVO.getSkuName()).stream()
                .map(SkuTableDO::getProductSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private PageResult<WarehouseStockRecordRespVO> buildPageResult(PageResult<WarehouseStockRecordDO> pageResult) {
        Set<String> warehouseIds = pageResult.getList().stream().map(WarehouseStockRecordDO::getWarehouseId).collect(Collectors.toSet());
        Set<Long> productSkuIds = pageResult.getList().stream().map(WarehouseStockRecordDO::getProductSkuId).collect(Collectors.toSet());
        Map<String, WarehouseDO> warehouseMap = getWarehouseMap(warehouseIds);
        Map<Long, SkuTableDO> skuMap = getSkuMap(productSkuIds);
        return BeanUtils.toBean(pageResult, WarehouseStockRecordRespVO.class,
                item -> fillRespVO(item, warehouseMap, skuMap));
    }

    private WarehouseStockRecordRespVO buildRespVO(WarehouseStockRecordDO stockRecord,
                                                   Map<String, WarehouseDO> warehouseMap,
                                                   Map<Long, SkuTableDO> skuMap) {
        WarehouseStockRecordRespVO respVO = BeanUtils.toBean(stockRecord, WarehouseStockRecordRespVO.class);
        fillRespVO(respVO, warehouseMap, skuMap);
        return respVO;
    }

    private void fillRespVO(WarehouseStockRecordRespVO respVO,
                            Map<String, WarehouseDO> warehouseMap,
                            Map<Long, SkuTableDO> skuMap) {
        WarehouseDO warehouse = warehouseMap.get(respVO.getWarehouseId());
        if (warehouse != null) {
            respVO.setWarehouseName(warehouse.getWarehouseName());
        }
        SkuTableDO sku = skuMap.get(respVO.getProductSkuId());
        if (sku != null) {
            respVO.setSkuCode(sku.getProductSkuCode());
            respVO.setSkuName(sku.getProductSkuName());
        }
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

}
