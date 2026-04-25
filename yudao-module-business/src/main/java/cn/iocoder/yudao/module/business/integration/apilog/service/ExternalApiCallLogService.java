package cn.iocoder.yudao.module.business.integration.apilog.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogPageReqVO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject.ExternalApiCallLogDO;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;

public interface ExternalApiCallLogService {

    Long createExternalApiCallLog(ExternalApiCallLogCreateReqBO createReqBO);

    PageResult<ExternalApiCallLogDO> getExternalApiCallLogPage(ExternalApiCallLogPageReqVO pageReqVO);
}
