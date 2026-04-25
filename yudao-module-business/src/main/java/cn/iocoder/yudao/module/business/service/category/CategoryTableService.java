package cn.iocoder.yudao.module.business.service.category;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.business.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.category.CategoryTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 商品类目表（三级树形结构） Service 接口
 *
 * @author 彼岸花
 */
public interface CategoryTableService {

    /**
     * 创建商品类目表（三级树形结构）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCategoryTable(@Valid CategoryTableSaveReqVO createReqVO);

    /**
     * 更新商品类目表（三级树形结构）
     *
     * @param updateReqVO 更新信息
     */
    void updateCategoryTable(@Valid CategoryTableSaveReqVO updateReqVO);

    /**
     * 删除商品类目表（三级树形结构）
     *
     * @param id 编号
     */
    void deleteCategoryTable(Long id);


    /**
     * 获得商品类目表（三级树形结构）
     *
     * @param id 编号
     * @return 商品类目表（三级树形结构）
     */
    CategoryTableDO getCategoryTable(Long id);

    /**
     * 获得商品类目表（三级树形结构）列表
     *
     * @param listReqVO 查询条件
     * @return 商品类目表（三级树形结构）列表
     */
    List<CategoryTableDO> getCategoryTableList(CategoryTableListReqVO listReqVO);

    /**
     * 导入门类目列表
     *
     * @param importList 导入列表
     * @param isUpdateSupport 是否支持更新
     * @return 导入结果
     */
    CategoryTableImportRespVO importCategoryTableList(List<CategoryTableImportReqVO> importList, boolean isUpdateSupport);

    /**
     * 批量删除商品类目表（三级树形结构）
     *
     * @param ids ID列表
     * @return 删除数量
     */
    int deleteCategoryTableByIds(List<Long> ids);

}