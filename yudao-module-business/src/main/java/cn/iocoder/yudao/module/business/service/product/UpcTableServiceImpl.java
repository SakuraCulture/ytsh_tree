package cn.iocoder.yudao.module.business.service.product;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.business.dal.mysql.product.UpcTableMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

@Service
@Validated
public class UpcTableServiceImpl implements UpcTableService {

    @Resource
    private UpcTableMapper upcTableMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpcTable(UpcTableSaveReqVO createReqVO) {
        validateUpcValueUnique(null, createReqVO.getProductUpcValue());
        if (Boolean.TRUE.equals(createReqVO.getProductUpcIsPrimary())) {
            clearPrimaryFlag(createReqVO.getProductSkuId());
        }
        if (createReqVO.getProductUpcStatus() == null) {
            createReqVO.setProductUpcStatus(1);
        }
        UpcTableDO upcTable = BeanUtils.toBean(createReqVO, UpcTableDO.class);
        upcTableMapper.insert(upcTable);
        return upcTable.getProductUpcId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUpcTable(UpcTableSaveReqVO updateReqVO) {
        validateUpcTableExists(updateReqVO.getProductUpcId());
        validateUpcValueUnique(updateReqVO.getProductUpcId(), updateReqVO.getProductUpcValue());
        if (Boolean.TRUE.equals(updateReqVO.getProductUpcIsPrimary())) {
            clearPrimaryFlag(updateReqVO.getProductSkuId());
        }
        UpcTableDO updateObj = BeanUtils.toBean(updateReqVO, UpcTableDO.class);
        upcTableMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUpcTable(Long id) {
        validateUpcTableExists(id);
        upcTableMapper.deleteById(id);
    }

    @Override
    public UpcTableDO getUpcTable(Long id) {
        return upcTableMapper.selectById(id);
    }

    @Override
    public PageResult<UpcTableDO> getUpcTablePage(UpcTablePageReqVO pageReqVO) {
        return upcTableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<UpcTableDO> getUpcTableListByProductSkuId(Long productSkuId) {
        return upcTableMapper.selectListByProductSkuId(productSkuId);
    }

    private void validateUpcTableExists(Long id) {
        if (upcTableMapper.selectById(id) == null) {
            throw exception(UPC_TABLE_NOT_EXISTS);
        }
    }

    private void validateUpcValueUnique(Long id, String value) {
        UpcTableDO existing = upcTableMapper.selectOne(UpcTableDO::getProductUpcValue, value);
        if (existing != null && !ObjectUtil.equal(existing.getProductUpcId(), id)) {
            throw exception(UPC_VALUE_EXISTS);
        }
    }

    private void clearPrimaryFlag(Long productSkuId) {
        List<UpcTableDO> list = upcTableMapper.selectListByProductSkuId(productSkuId);
        for (UpcTableDO upc : list) {
            if (Boolean.TRUE.equals(upc.getProductUpcIsPrimary())) {
                upc.setProductUpcIsPrimary(0);
                upcTableMapper.updateById(upc);
            }
        }
    }

}
