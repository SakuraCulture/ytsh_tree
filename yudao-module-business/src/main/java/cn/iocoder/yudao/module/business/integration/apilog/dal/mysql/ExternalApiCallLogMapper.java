package cn.iocoder.yudao.module.business.integration.apilog.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogPageReqVO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject.ExternalApiCallLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalApiCallLogMapper extends BaseMapperX<ExternalApiCallLogDO> {

    default PageResult<ExternalApiCallLogDO> selectPage(ExternalApiCallLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExternalApiCallLogDO>()
                .eqIfPresent(ExternalApiCallLogDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(ExternalApiCallLogDO::getApiCode, reqVO.getApiCode())
                .eqIfPresent(ExternalApiCallLogDO::getBizType, reqVO.getBizType())
                .eqIfPresent(ExternalApiCallLogDO::getBizId, reqVO.getBizId())
                .eqIfPresent(ExternalApiCallLogDO::getTraceId, reqVO.getTraceId())
                .eqIfPresent(ExternalApiCallLogDO::getExternalTraceId, reqVO.getExternalTraceId())
                .eqIfPresent(ExternalApiCallLogDO::getOrderId, reqVO.getOrderId())
                .orderByDesc(ExternalApiCallLogDO::getId));
    }
}
