package cn.iocoder.yudao.module.business.service.product;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * SPU基础分类 Service 接口
 *
 * @author 芋道源码
 */
public interface SpuTableService {

    /**
     * 创建SPU基础分类
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSpuTable(@Valid SpuTableSaveReqVO createReqVO);

    /**
     * 更新SPU基础分类
     *
     * @param updateReqVO 更新信息
     */
    void updateSpuTable(@Valid SpuTableSaveReqVO updateReqVO);

    /**
     * 删除SPU基础分类
     *
     * @param id 编号
     */
    void deleteSpuTable(Long id);

    /**
    * 批量删除SPU基础分类
    *
    * @param ids 编号
    */
    void deleteSpuTableListByIds(List<Long> ids);

    /**
     * 获得SPU基础分类
     *
     * @param id 编号
     * @return SPU基础分类
     */
    SpuTableDO getSpuTable(Long id);

    /**
     * 获得SPU基础分类分页
     *
     * @param pageReqVO 分页查询
     * @return SPU基础分类分页
     */
    PageResult<SpuTableDO> getSpuTablePage(SpuTablePageReqVO pageReqVO);

    // ==================== 子表（SKU商品主数据） ====================

    /**
     * 获得SKU商品主数据列表
     *
     * @param productSpuId 所属SPU
     * @return SKU商品主数据列表
     */
    List<SkuTableDO> getSkuTableListByProductSpuId(Long productSpuId);

    /**
     * 导入SPU/SKU/UPC数据
     *
     * @param list 导入数据列表
     * @param updateSupport 是否支持更新
     * @return 导入结果
     */
    SpuImportRespVO importSpuSkuUpcList(List<SpuSkuUpcImportVO> list, Boolean updateSupport);

    /**
     * 获取导出用的SPU/SKU/UPC数据
     *
     * @param pageReqVO 分页查询
     * @return 导出数据列表
     */
    List<SpuSkuUpcExportVO> getExportData(SpuTablePageReqVO pageReqVO);

}