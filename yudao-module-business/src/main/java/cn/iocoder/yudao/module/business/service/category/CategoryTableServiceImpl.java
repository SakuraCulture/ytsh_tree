package cn.iocoder.yudao.module.business.service.category;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.business.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.category.CategoryTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.business.dal.mysql.category.CategoryTableMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

/**
 * 商品类目表（三级树形结构） Service 实现类
 *
 * @author 彼岸花
 */
@Service
@Validated
public class CategoryTableServiceImpl implements CategoryTableService {

    @Resource
    private CategoryTableMapper categoryTableMapper;

    @Override
    public Long createCategoryTable(CategoryTableSaveReqVO createReqVO) {
        // 校验父类目ID（0表示一级类目）的有效性
        validateParentCategoryTable(null, createReqVO.getParentId());
        // 校验类目名称的唯一性
        validateCategoryTableCategoryNameUnique(null, createReqVO.getParentId(), createReqVO.getCategoryName());

        // 插入
        CategoryTableDO categoryTable = BeanUtils.toBean(createReqVO, CategoryTableDO.class);
        categoryTable.setTenantId(1L);
        categoryTableMapper.insert(categoryTable);

        // 返回
        return categoryTable.getCategoryId();
    }

    @Override
    public void updateCategoryTable(CategoryTableSaveReqVO updateReqVO) {
        // 校验存在
        validateCategoryTableExists(updateReqVO.getCategoryId());
        // 校验父类目ID（0表示一级类目）的有效性
        validateParentCategoryTable(updateReqVO.getCategoryId(), updateReqVO.getParentId());
        // 校验类目名称的唯一性
        validateCategoryTableCategoryNameUnique(updateReqVO.getCategoryId(), updateReqVO.getParentId(), updateReqVO.getCategoryName());

        // 更新
        CategoryTableDO updateObj = BeanUtils.toBean(updateReqVO, CategoryTableDO.class);
        categoryTableMapper.updateById(updateObj);
    }

    @Override
    public void deleteCategoryTable(Long id) {
        // 校验存在
        validateCategoryTableExists(id);
        // 校验是否有子商品类目表（三级树形结构）
        if (categoryTableMapper.selectCountByParentId(id) > 0) {
            throw exception(CATEGORY_TABLE_EXITS_CHILDREN);
        }
        // 删除
        categoryTableMapper.deleteById(id);
    }


    private void validateCategoryTableExists(Long id) {
        if (categoryTableMapper.selectById(id) == null) {
            throw exception(CATEGORY_TABLE_NOT_EXISTS);
        }
    }

    private void validateParentCategoryTable(Long id, Long parentId) {
        if (parentId == null || CategoryTableDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父商品类目表（三级树形结构）
        if (Objects.equals(id, parentId)) {
            throw exception(CATEGORY_TABLE_PARENT_ERROR);
        }
        // 2. 父商品类目表（三级树形结构）不存在
        CategoryTableDO parentCategoryTable = categoryTableMapper.selectById(parentId);
        if (parentCategoryTable == null) {
            throw exception(CATEGORY_TABLE_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父商品类目表（三级树形结构），如果父商品类目表（三级树形结构）是自己的子商品类目表（三级树形结构），则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentCategoryTable.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(CATEGORY_TABLE_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父商品类目表（三级树形结构）
            if (parentId == null || CategoryTableDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentCategoryTable = categoryTableMapper.selectById(parentId);
            if (parentCategoryTable == null) {
                break;
            }
        }
    }

    private void validateCategoryTableCategoryNameUnique(Long id, Long parentId, String categoryName) {
        CategoryTableDO categoryTable = categoryTableMapper.selectByParentIdAndCategoryName(parentId, categoryName);
        if (categoryTable == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的商品类目表（三级树形结构）
        if (id == null) {
            throw exception(CATEGORY_TABLE_CATEGORY_NAME_DUPLICATE);
        }
        if (!Objects.equals(categoryTable.getCategoryId(), id)) {
            throw exception(CATEGORY_TABLE_CATEGORY_NAME_DUPLICATE);
        }
    }

    @Override
    public CategoryTableDO getCategoryTable(Long id) {
        return categoryTableMapper.selectById(id);
    }

    @Override
    public List<CategoryTableDO> getCategoryTableList(CategoryTableListReqVO listReqVO) {
        List<CategoryTableDO> list = categoryTableMapper.selectList(listReqVO);
        if (CollUtil.isEmpty(list)) {
            return list;
        }
        Set<Long> parentIds = list.stream()
                .map(CategoryTableDO::getParentId)
                .filter(parentId -> parentId != null && !parentId.equals(CategoryTableDO.PARENT_ID_ROOT))
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(parentIds)) {
            List<CategoryTableDO> parentList = categoryTableMapper.selectBatchIds(parentIds);
            Map<Long, String> parentNameMap = parentList.stream()
                    .collect(Collectors.toMap(CategoryTableDO::getCategoryId, CategoryTableDO::getCategoryName));
            for (CategoryTableDO item : list) {
                if (item.getParentId() != null && !item.getParentId().equals(CategoryTableDO.PARENT_ID_ROOT)) {
                    item.setParentCategoryName(parentNameMap.get(item.getParentId()));
                }
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryTableImportRespVO importCategoryTableList(List<CategoryTableImportReqVO> importList, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importList)) {
            throw exception(CATEGORY_TABLE_IMPORT_LIST_IS_EMPTY);
        }

        CategoryTableImportRespVO respVO = CategoryTableImportRespVO.builder()
                .createCategoryNames(new ArrayList<>())
                .updateCategoryNames(new ArrayList<>())
                .failureCategoryNames(new LinkedHashMap<>())
                .build();

        // 构建现有类目名称->ID的映射（用于父类目名称解析）
        List<CategoryTableDO> existList = categoryTableMapper.selectList();
        Map<String, CategoryTableDO> existByNameMap = new HashMap<>();
        for (CategoryTableDO item : existList) {
            existByNameMap.put(item.getCategoryName(), item);
        }

        // 遍历导入数据
        for (CategoryTableImportReqVO importItem : importList) {
            try {
                // 校验必填字段
                if (StrUtil.isBlank(importItem.getCategoryName())) {
                    respVO.getFailureCategoryNames().put(importItem.getCategoryName(), "类目名称不能为空");
                    continue;
                }
                if (importItem.getCategoryLevel() == null) {
                    respVO.getFailureCategoryNames().put(importItem.getCategoryName(), "层级不能为空");
                    continue;
                }
                // 校验层级范围
                if (importItem.getCategoryLevel() < 1 || importItem.getCategoryLevel() > 3) {
                    respVO.getFailureCategoryNames().put(importItem.getCategoryName(), "层级超出范围(只支持1-3级)");
                    continue;
                }

                // 解析父类目ID
                Long parentId = CategoryTableDO.PARENT_ID_ROOT;
                if (StrUtil.isNotBlank(importItem.getParentCategoryName())) {
                    CategoryTableDO parentCategory = existByNameMap.get(importItem.getParentCategoryName());
                    if (parentCategory == null) {
                        respVO.getFailureCategoryNames().put(importItem.getCategoryName(), "父类目[" + importItem.getParentCategoryName() + "]不存在");
                        continue;
                    }
                    parentId = parentCategory.getCategoryId();
                }

                // 检查是否已存在（按父ID+名称唯一）
                CategoryTableDO existCategory = categoryTableMapper.selectByParentIdAndCategoryName(parentId, importItem.getCategoryName());

                if (existCategory != null) {
                    if (isUpdateSupport) {
                        // 更新模式
                        existCategory.setCategoryLevel(importItem.getCategoryLevel());
                        if (importItem.getSortOrder() != null) {
                            existCategory.setSortOrder(importItem.getSortOrder());
                        }
                        if (importItem.getStatus() != null) {
                            existCategory.setStatus(importItem.getStatus());
                        }
                        categoryTableMapper.updateById(existCategory);
                        respVO.getUpdateCategoryNames().add(importItem.getCategoryName());
                    } else {
                        // 非更新模式，跳过
                        continue;
                    }
                } else {
                    // 新增
                    CategoryTableDO category = CategoryTableDO.builder()
                            .tenantId(1L)
                            .categoryName(importItem.getCategoryName())
                            .parentId(parentId)
                            .categoryLevel(importItem.getCategoryLevel())
                            .sortOrder(importItem.getSortOrder() != null ? importItem.getSortOrder() : 0)
                            .status(importItem.getStatus() != null ? importItem.getStatus() : 1)
                            .build();
                    categoryTableMapper.insert(category);
                    respVO.getCreateCategoryNames().add(importItem.getCategoryName());
                }
            } catch (Exception e) {
                respVO.getFailureCategoryNames().put(importItem.getCategoryName(), "导入失败: " + e.getMessage());
            }
        }

        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryTableByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        // 递归收集所有子孙节点 ID
        Set<Long> allIds = new HashSet<>();
        collectDescendantIds(ids, allIds);
        // 批量删除
        if (CollUtil.isNotEmpty(allIds)) {
            categoryTableMapper.deleteBatchIds(allIds);
        }
        return allIds.size();
    }

    private void collectDescendantIds(List<Long> parentIds, Set<Long> allIds) {
        if (CollUtil.isEmpty(parentIds)) {
            return;
        }
        allIds.addAll(parentIds);
        // 查找这些父节点的直接子节点
        List<CategoryTableDO> children = categoryTableMapper.selectListByParentIds(parentIds);
        if (CollUtil.isEmpty(children)) {
            return;
        }
        // 递归收集子节点的子节点
        List<Long> childIds = convertList(children, CategoryTableDO::getCategoryId);
        collectDescendantIds(childIds, allIds);
    }

}