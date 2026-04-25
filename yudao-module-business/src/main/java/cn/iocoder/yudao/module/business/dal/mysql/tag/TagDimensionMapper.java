package cn.iocoder.yudao.module.business.dal.mysql.tag;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagDimensionMapper extends BaseMapperX<TagDimensionDO> {

    default TagDimensionDO selectByDomainTypeAndParentIdAndCode(String domainType, Long parentId, String code) {
        return selectOne(new LambdaQueryWrapperX<TagDimensionDO>()
                .eq(TagDimensionDO::getDomainType, domainType)
                .eq(TagDimensionDO::getParentId, parentId)
                .eq(TagDimensionDO::getCode, code));
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(TagDimensionDO::getParentId, parentId);
    }

    default List<TagDimensionDO> selectList(String domainType, Long parentId, Integer level) {
        return selectList(new LambdaQueryWrapperX<TagDimensionDO>()
                .eqIfPresent(TagDimensionDO::getDomainType, domainType)
                .eqIfPresent(TagDimensionDO::getParentId, parentId)
                .eqIfPresent(TagDimensionDO::getLevel, level)
                .orderByAsc(TagDimensionDO::getSort)
                .orderByAsc(TagDimensionDO::getId));
    }

    @Select("SELECT * FROM tag_dimension WHERE id = #{id}")
    TagDimensionDO selectByIdIgnoreDeleted(@Param("id") Long id);

}
