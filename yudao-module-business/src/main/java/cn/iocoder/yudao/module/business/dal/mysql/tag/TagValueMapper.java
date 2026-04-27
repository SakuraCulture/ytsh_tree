package cn.iocoder.yudao.module.business.dal.mysql.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValuePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;

@Mapper
public interface TagValueMapper extends BaseMapperX<TagValueDO> {

    default TagValueDO selectByDimensionIdAndCode(Long dimensionId, String code) {
        return selectOne(new LambdaQueryWrapperX<TagValueDO>()
                .eq(TagValueDO::getDimensionId, dimensionId)
                .eq(TagValueDO::getCode, code));
    }

    default List<TagValueDO> selectListByDimensionId(Long dimensionId) {
        return selectList(new LambdaQueryWrapperX<TagValueDO>()
                .eq(TagValueDO::getDimensionId, dimensionId)
                .orderByAsc(TagValueDO::getSort)
                .orderByAsc(TagValueDO::getId));
    }

    default Long selectCountByDimensionId(Long dimensionId) {
        return selectCount(TagValueDO::getDimensionId, dimensionId);
    }

    default List<TagValueDO> selectEnabledListByDimensionIds(List<Long> dimensionIds) {
        return selectList(new LambdaQueryWrapperX<TagValueDO>()
                .inIfPresent(TagValueDO::getDimensionId, dimensionIds)
                .eq(TagValueDO::getStatus, STATUS_ENABLED)
                .orderByAsc(TagValueDO::getSort)
                .orderByAsc(TagValueDO::getId));
    }

    default PageResult<TagValueDO> selectPage(TagValuePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TagValueDO>()
                .eqIfPresent(TagValueDO::getDimensionId, reqVO.getDimensionId())
                .likeIfPresent(TagValueDO::getName, reqVO.getName())
                .likeIfPresent(TagValueDO::getCode, reqVO.getCode())
                .eqIfPresent(TagValueDO::getTagMethod, reqVO.getTagMethod())
                .eqIfPresent(TagValueDO::getStatus, reqVO.getStatus())
                .orderByDesc(TagValueDO::getId));
    }

    default PageResult<TagValueDO> selectPage(TagValuePageReqVO reqVO, List<Long> dimensionIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TagValueDO>()
                .eqIfPresent(TagValueDO::getDimensionId, reqVO.getDimensionId())
                .inIfPresent(TagValueDO::getDimensionId, dimensionIds)
                .likeIfPresent(TagValueDO::getName, reqVO.getName())
                .likeIfPresent(TagValueDO::getCode, reqVO.getCode())
                .eqIfPresent(TagValueDO::getTagMethod, reqVO.getTagMethod())
                .eqIfPresent(TagValueDO::getStatus, reqVO.getStatus())
                .orderByDesc(TagValueDO::getId));
    }

    @Select("SELECT * FROM tag_value WHERE id = #{id}")
    TagValueDO selectByIdIgnoreDeleted(@Param("id") Long id);

}
