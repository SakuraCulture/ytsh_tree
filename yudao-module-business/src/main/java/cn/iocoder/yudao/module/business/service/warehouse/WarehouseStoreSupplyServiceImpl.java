package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplySaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStoreSupplyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.STORE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_STORE_SUPPLY_DUPLICATE;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_STORE_SUPPLY_NOT_EXISTS;

@Service
@Validated
public class WarehouseStoreSupplyServiceImpl implements WarehouseStoreSupplyService {

    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private StoreMapper storeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouseStoreSupply(WarehouseStoreSupplySaveReqVO createReqVO) {
        validateWarehouseExists(createReqVO.getWarehouseId());
        validateStoreExists(createReqVO.getStoreId());
        validateDuplicate(null, createReqVO.getWarehouseId(), createReqVO.getStoreId());
        WarehouseStoreSupplyDO relation = BeanUtils.toBean(createReqVO, WarehouseStoreSupplyDO.class);
        warehouseStoreSupplyMapper.insert(relation);
        if (Integer.valueOf(1).equals(createReqVO.getIsPrimary())) {
            clearOtherPrimaryFlags(relation.getId(), createReqVO.getStoreId());
        }
        return relation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseStoreSupply(WarehouseStoreSupplySaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateWarehouseExists(updateReqVO.getWarehouseId());
        validateStoreExists(updateReqVO.getStoreId());
        validateDuplicate(updateReqVO.getId(), updateReqVO.getWarehouseId(), updateReqVO.getStoreId());
        WarehouseStoreSupplyDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseStoreSupplyDO.class);
        warehouseStoreSupplyMapper.updateById(updateObj);
        if (Integer.valueOf(1).equals(updateReqVO.getIsPrimary())) {
            clearOtherPrimaryFlags(updateReqVO.getId(), updateReqVO.getStoreId());
        }
    }

    @Override
    public void deleteWarehouseStoreSupply(Long id) {
        validateExists(id);
        warehouseStoreSupplyMapper.deleteById(id);
    }

    @Override
    public WarehouseStoreSupplyRespVO getWarehouseStoreSupply(Long id) {
        return buildRespVO(validateExists(id));
    }

    @Override
    public PageResult<WarehouseStoreSupplyRespVO> getWarehouseStoreSupplyPage(WarehouseStoreSupplyPageReqVO pageReqVO) {
        PageResult<WarehouseStoreSupplyDO> pageResult = warehouseStoreSupplyMapper.selectPageRaw(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return new PageResult<>(pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()), pageResult.getTotal());
    }

    @Override
    public List<WarehouseStoreSupplyRespVO> getWarehouseStoreSupplySimpleList(String warehouseId) {
        return warehouseStoreSupplyMapper.selectListByWarehouseId(warehouseId).stream().map(this::buildRespVO).collect(Collectors.toList());
    }

    @Override
    public WarehouseStoreSupplyImportRespVO importWarehouseStoreSupplyList(List<WarehouseStoreSupplyImportExcelVO> importList, boolean updateSupport) {
        WarehouseStoreSupplyImportRespVO respVO = WarehouseStoreSupplyImportRespVO.builder()
                .createCount(0)
                .updateCount(0)
                .failureCount(0)
                .failureRows(new LinkedHashMap<>())
                .build();
        if (CollUtil.isEmpty(importList)) {
            return respVO;
        }

        for (int i = 0; i < importList.size(); i++) {
            WarehouseStoreSupplyImportExcelVO row = importList.get(i);
            String rowKey = "第 " + (i + 1) + " 行";
            try {
                if (row.getWarehouseId() == null || row.getWarehouseId().isBlank() || row.getStoreId() == null || row.getStoreId().isBlank()) {
                    throw new IllegalArgumentException("仓库ID和门店ID不能为空");
                }
                validateWarehouseExists(row.getWarehouseId());
                validateStoreExists(row.getStoreId());

                Integer isPrimary = row.getIsPrimary() != null ? row.getIsPrimary() : 0;
                Integer supplyStatus = row.getSupplyStatus() != null ? row.getSupplyStatus() : 1;
                WarehouseStoreSupplyDO existing = warehouseStoreSupplyMapper.selectByWarehouseIdAndStoreId(row.getWarehouseId(), row.getStoreId());
                if (existing == null) {
                    WarehouseStoreSupplyDO relation = new WarehouseStoreSupplyDO();
                    relation.setWarehouseId(row.getWarehouseId());
                    relation.setStoreId(row.getStoreId());
                    relation.setIsPrimary(isPrimary);
                    relation.setSupplyStatus(supplyStatus);
                    relation.setRemark(row.getRemark());
                    warehouseStoreSupplyMapper.insert(relation);
                    if (Integer.valueOf(1).equals(isPrimary)) {
                        clearOtherPrimaryFlags(relation.getId(), relation.getStoreId());
                    }
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                    continue;
                }
                if (!updateSupport) {
                    throw exception(WAREHOUSE_STORE_SUPPLY_DUPLICATE);
                }
                existing.setIsPrimary(isPrimary);
                existing.setSupplyStatus(supplyStatus);
                existing.setRemark(row.getRemark());
                warehouseStoreSupplyMapper.updateById(existing);
                if (Integer.valueOf(1).equals(isPrimary)) {
                    clearOtherPrimaryFlags(existing.getId(), existing.getStoreId());
                }
                respVO.setUpdateCount(respVO.getUpdateCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureRows().put(rowKey, ex.getMessage());
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    private WarehouseStoreSupplyDO validateExists(Long id) {
        WarehouseStoreSupplyDO relation = warehouseStoreSupplyMapper.selectById(id);
        if (relation == null) {
            throw exception(WAREHOUSE_STORE_SUPPLY_NOT_EXISTS);
        }
        return relation;
    }

    private void validateWarehouseExists(String warehouseId) {
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
    }

    private void validateStoreExists(String storeId) {
        if (storeMapper.selectById(storeId) == null) {
            throw exception(STORE_NOT_EXISTS);
        }
    }

    private void validateDuplicate(Long id, String warehouseId, String storeId) {
        WarehouseStoreSupplyDO relation = warehouseStoreSupplyMapper.selectByWarehouseIdAndStoreId(warehouseId, storeId);
        if (relation != null && !relation.getId().equals(id)) {
            throw exception(WAREHOUSE_STORE_SUPPLY_DUPLICATE);
        }
    }

    private void clearOtherPrimaryFlags(Long keepId, String storeId) {
        warehouseStoreSupplyMapper.selectListByStoreId(storeId).stream()
                .filter(item -> !item.getId().equals(keepId) && Integer.valueOf(1).equals(item.getIsPrimary()))
                .forEach(item -> {
                    item.setIsPrimary(0);
                    warehouseStoreSupplyMapper.updateById(item);
                });
    }

    private WarehouseStoreSupplyRespVO buildRespVO(WarehouseStoreSupplyDO relation) {
        WarehouseStoreSupplyRespVO respVO = BeanUtils.toBean(relation, WarehouseStoreSupplyRespVO.class);
        WarehouseDO warehouse = warehouseMapper.selectById(relation.getWarehouseId());
        StoreDO store = storeMapper.selectById(relation.getStoreId());
        if (warehouse != null) {
            respVO.setWarehouseName(warehouse.getWarehouseName());
        }
        if (store != null) {
            respVO.setStoreName(store.getStoreName());
        }
        return respVO;
    }
}
