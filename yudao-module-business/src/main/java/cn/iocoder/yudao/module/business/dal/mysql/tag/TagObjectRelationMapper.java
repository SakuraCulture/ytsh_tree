package cn.iocoder.yudao.module.business.dal.mysql.tag;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.RELATION_STATUS_ENABLED;

@Mapper
public interface TagObjectRelationMapper extends BaseMapperX<TagObjectRelationDO> {

    default List<TagObjectRelationDO> selectActiveList(String domainType, String objectType, String objectId) {
        return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
                .eq(TagObjectRelationDO::getDomainType, domainType)
                .eq(TagObjectRelationDO::getObjectType, objectType)
                .eq(TagObjectRelationDO::getObjectId, objectId)
                .eq(TagObjectRelationDO::getStatus, RELATION_STATUS_ENABLED)
                .orderByAsc(TagObjectRelationDO::getId));
    }

    default List<TagObjectRelationDO> selectActiveListByObjectIds(String domainType, String objectType, Collection<String> objectIds) {
        return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
                .eq(TagObjectRelationDO::getDomainType, domainType)
                .eq(TagObjectRelationDO::getObjectType, objectType)
                .inIfPresent(TagObjectRelationDO::getObjectId, objectIds)
                .eq(TagObjectRelationDO::getStatus, RELATION_STATUS_ENABLED));
    }

    default List<TagObjectRelationDO> selectActiveListByTagValue(String domainType, String objectType, Long tagValueId) {
        return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
                .eq(TagObjectRelationDO::getDomainType, domainType)
                .eq(TagObjectRelationDO::getObjectType, objectType)
                .eq(TagObjectRelationDO::getTagValueId, tagValueId)
                .eq(TagObjectRelationDO::getStatus, RELATION_STATUS_ENABLED)
                .orderByAsc(TagObjectRelationDO::getId));
    }

    default List<TagObjectRelationDO> selectListByObjectAndSource(String domainType, String objectType, String objectId,
                                                                  String sourceType, String sourceRef) {
        return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
                .eq(TagObjectRelationDO::getDomainType, domainType)
                .eq(TagObjectRelationDO::getObjectType, objectType)
                .eq(TagObjectRelationDO::getObjectId, objectId)
                .eq(TagObjectRelationDO::getSourceType, sourceType)
                .eq(TagObjectRelationDO::getSourceRef, sourceRef)
                .orderByAsc(TagObjectRelationDO::getId));
    }

    default TagObjectRelationDO selectByBiz(String domainType, String objectType, String objectId, Long tagValueId,
                                            String sourceType, String sourceRef) {
        return selectOne(new LambdaQueryWrapperX<TagObjectRelationDO>()
                .eq(TagObjectRelationDO::getDomainType, domainType)
                .eq(TagObjectRelationDO::getObjectType, objectType)
                .eq(TagObjectRelationDO::getObjectId, objectId)
                .eq(TagObjectRelationDO::getTagValueId, tagValueId)
                .eq(TagObjectRelationDO::getSourceType, sourceType)
                .eq(TagObjectRelationDO::getSourceRef, sourceRef));
    }

    @Select("SELECT * FROM tag_object_relation WHERE object_type = #{objectType} AND object_id = #{objectId} ORDER BY id ASC")
    List<TagObjectRelationDO> selectByObjectIdIgnoreDeleted(@Param("objectType") String objectType, @Param("objectId") String objectId);

}
