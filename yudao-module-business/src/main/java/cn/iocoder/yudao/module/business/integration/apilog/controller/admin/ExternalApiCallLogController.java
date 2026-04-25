package cn.iocoder.yudao.module.business.integration.apilog.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogPageReqVO;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogRespVO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject.ExternalApiCallLogDO;
import cn.iocoder.yudao.module.business.integration.apilog.service.ExternalApiCallLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 外部 API 调用日志")
@RestController
@RequestMapping("/business/external-api-call-log")
@Validated
public class ExternalApiCallLogController {

    @Resource
    private ExternalApiCallLogService externalApiCallLogService;

    @GetMapping("/page")
    @Operation(summary = "获得外部 API 调用日志分页")
    @PreAuthorize("@ss.hasPermission('business:external-api-call-log:query')")
    public CommonResult<PageResult<ExternalApiCallLogRespVO>> getExternalApiCallLogPage(@Valid ExternalApiCallLogPageReqVO pageReqVO) {
        PageResult<ExternalApiCallLogDO> pageResult = externalApiCallLogService.getExternalApiCallLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExternalApiCallLogRespVO.class));
    }
}
