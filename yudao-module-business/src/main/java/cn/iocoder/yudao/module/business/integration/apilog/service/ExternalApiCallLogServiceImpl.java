package cn.iocoder.yudao.module.business.integration.apilog.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogPageReqVO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject.ExternalApiCallLogDO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.mysql.ExternalApiCallLogMapper;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ExternalApiCallLogServiceImpl implements ExternalApiCallLogService {

    @Resource
    private ExternalApiCallLogMapper externalApiCallLogMapper;

    @Override
    public Long createExternalApiCallLog(ExternalApiCallLogCreateReqBO createReqBO) {
        ExternalApiCallLogDO log = BeanUtils.toBean(createReqBO, ExternalApiCallLogDO.class, item -> item.setDeleted(false));
        externalApiCallLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<ExternalApiCallLogDO> getExternalApiCallLogPage(ExternalApiCallLogPageReqVO pageReqVO) {
        return externalApiCallLogMapper.selectPage(pageReqVO);
    }
}
