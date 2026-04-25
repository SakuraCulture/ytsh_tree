package cn.iocoder.yudao.module.business.dal.mysql.category;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.category.CategoryTableDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.business.controller.admin.category.vo.*;

/**
 * 商品类目表（三级树形结构） Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface CategoryTableMapper extends BaseMapperX<CategoryTableDO> {

    default List<CategoryTableDO> selectList(CategoryTableListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CategoryTableDO>()
                .eqIfPresent(CategoryTableDO::getCategoryCode, reqVO.getCategoryCode())
                .likeIfPresent(CategoryTableDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(CategoryTableDO::getParentId, reqVO.getParentId())
                .eqIfPresent(CategoryTableDO::getCategoryLevel, reqVO.getCategoryLevel())
                .eqIfPresent(CategoryTableDO::getCategoryPath, reqVO.getCategoryPath())
                .eqIfPresent(CategoryTableDO::getCategoryIcon, reqVO.getCategoryIcon())
                .eqIfPresent(CategoryTableDO::getCategoryImage, reqVO.getCategoryImage())
                .eqIfPresent(CategoryTableDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(CategoryTableDO::getIsLeaf, reqVO.getIsLeaf())
                .eqIfPresent(CategoryTableDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CategoryTableDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CategoryTableDO::getCategoryId));
    }

	default CategoryTableDO selectByParentIdAndCategoryName(Long parentId, String categoryName) {
	    return selectOne(CategoryTableDO::getParentId, parentId, CategoryTableDO::getCategoryName, categoryName);
	}

    default Long selectCountByParentId(Long parentId) {
        return selectCount(CategoryTableDO::getParentId, parentId);
    }

    default List<CategoryTableDO> selectListByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CategoryTableDO>()
                .in(CategoryTableDO::getParentId, parentIds));
    }

}