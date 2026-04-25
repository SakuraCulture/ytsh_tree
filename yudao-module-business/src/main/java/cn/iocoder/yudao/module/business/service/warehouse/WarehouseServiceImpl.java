package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

@Service
@Validated
public class WarehouseServiceImpl implements WarehouseService {

    @Resource
    private WarehouseMapper warehouseMapper;

    @Override
    public String createWarehouse(WarehouseSaveReqVO createReqVO) {
        validateWarehouseCodeUnique(null, createReqVO.getWarehouseCode());
        validateWarehouseNameUnique(null, createReqVO.getWarehouseName());
        WarehouseDO warehouse = BeanUtils.toBean(createReqVO, WarehouseDO.class);
        if (warehouse.getIsDefault() == null) {
            warehouse.setIsDefault(0);
        }
        warehouseMapper.insert(warehouse);
        if (Integer.valueOf(1).equals(warehouse.getIsDefault())) {
            updateWarehouseDefaultStatus(warehouse.getWarehouseId(), 1);
        }
        return warehouse.getWarehouseId();
    }

    @Override
    public void updateWarehouse(WarehouseSaveReqVO updateReqVO) {
        WarehouseDO existWarehouse = validateWarehouseExists(updateReqVO.getWarehouseId());
        validateWarehouseCodeUnique(existWarehouse.getWarehouseId(), updateReqVO.getWarehouseCode());
        validateWarehouseNameUnique(existWarehouse.getWarehouseId(), updateReqVO.getWarehouseName());
        WarehouseDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseDO.class);
        warehouseMapper.updateById(updateObj);
        if (Integer.valueOf(1).equals(updateReqVO.getIsDefault())) {
            updateWarehouseDefaultStatus(updateReqVO.getWarehouseId(), 1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseDefaultStatus(String warehouseId, Integer isDefault) {
        validateWarehouseExists(warehouseId);
        if (Integer.valueOf(1).equals(isDefault)) {
            WarehouseDO defaultWarehouse = warehouseMapper.selectByDefaultWarehouse();
            if (defaultWarehouse != null && !defaultWarehouse.getWarehouseId().equals(warehouseId)) {
                warehouseMapper.updateById(WarehouseDO.builder()
                        .warehouseId(defaultWarehouse.getWarehouseId())
                        .isDefault(0)
                        .build());
            }
        }
        warehouseMapper.updateById(WarehouseDO.builder()
                .warehouseId(warehouseId)
                .isDefault(isDefault)
                .build());
    }

    @Override
    public void deleteWarehouse(String warehouseId) {
        validateWarehouseExists(warehouseId);
        warehouseMapper.deleteById(warehouseId);
    }

    @Override
    public WarehouseDO getWarehouse(String warehouseId) {
        return warehouseMapper.selectById(warehouseId);
    }

    @Override
    public PageResult<WarehouseDO> getWarehousePage(WarehousePageReqVO pageReqVO) {
        return warehouseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WarehouseSimpleRespVO> getWarehouseSimpleList() {
        List<WarehouseDO> list = warehouseMapper.selectListByWarehouseStatus(1);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, WarehouseSimpleRespVO.class);
    }

    @Override
    public List<WarehouseDO> getWarehouseList(Collection<String> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectListByWarehouseIds(warehouseIds);
    }

    @Override
    public WarehouseDO validateWarehouseExists(String warehouseId) {
        WarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        return warehouse;
    }

    private void validateWarehouseCodeUnique(String warehouseId, String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isEmpty()) {
            return;
        }
        WarehouseDO warehouse = warehouseMapper.selectByWarehouseCode(warehouseCode);
        if (warehouse != null && !warehouse.getWarehouseId().equals(warehouseId)) {
            throw exception(WAREHOUSE_CODE_EXISTS);
        }
    }

    private void validateWarehouseNameUnique(String warehouseId, String warehouseName) {
        if (warehouseName == null || warehouseName.isEmpty()) {
            return;
        }
        WarehouseDO warehouse = warehouseMapper.selectByWarehouseName(warehouseName);
        if (warehouse != null && !warehouse.getWarehouseId().equals(warehouseId)) {
            throw exception(WAREHOUSE_NAME_EXISTS);
        }
    }

}
