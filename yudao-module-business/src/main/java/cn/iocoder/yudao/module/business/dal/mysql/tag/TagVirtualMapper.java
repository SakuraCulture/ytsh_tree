package cn.iocoder.yudao.module.business.dal.mysql.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagVirtualDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TagVirtualMapper extends BaseMapperX<TagVirtualDO> {

    default TagVirtualDO selectByDomainTypeAndCode(String domainType, String code) {
        return selectOne(new LambdaQueryWrapperX<TagVirtualDO>()
                .eq(TagVirtualDO::getDomainType, domainType)
                .eq(TagVirtualDO::getCode, code));
    }

    default PageResult<TagVirtualDO> selectPage(TagVirtualPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TagVirtualDO>()
                .eqIfPresent(TagVirtualDO::getDomainType, reqVO.getDomainType())
                .likeIfPresent(TagVirtualDO::getName, reqVO.getName())
                .likeIfPresent(TagVirtualDO::getCode, reqVO.getCode())
                .eqIfPresent(TagVirtualDO::getStatus, reqVO.getStatus())
                .orderByDesc(TagVirtualDO::getId));
    }

    /**
     * 仅用于测试校验逻辑删除后的 uniqueDeleted。
     */
    @Select("SELECT * FROM tag_virtual WHERE id = #{id}")
    TagVirtualDO selectByIdIgnoreDeleted(@Param("id") Long id);

}
