package cn.iocoder.yudao.module.business.service.product;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

public interface UpcTableService {

    Long createUpcTable(@Valid UpcTableSaveReqVO createReqVO);

    void updateUpcTable(@Valid UpcTableSaveReqVO updateReqVO);

    void deleteUpcTable(Long id);

    UpcTableDO getUpcTable(Long id);

    PageResult<UpcTableDO> getUpcTablePage(UpcTablePageReqVO pageReqVO);

    List<UpcTableDO> getUpcTableListByProductSkuId(Long productSkuId);

}
