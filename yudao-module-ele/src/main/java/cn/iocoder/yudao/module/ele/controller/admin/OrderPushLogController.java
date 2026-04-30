package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderPushLogRespVO;
import cn.iocoder.yudao.module.ele.service.OrderPushLogService;
import cn.iocoder.yudao.module.ele.service.dto.OrderPushLogDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "管理后台 - 订单推送日志")
@RestController
@RequestMapping("/ele/push-log")
@Validated
public class OrderPushLogController {

    @Resource
    private OrderPushLogService orderPushLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询推送日志")
    public CommonResult<PageResult<OrderPushLogRespVO>> getPushLogPage(
            @Parameter(description = "订单号") @RequestParam(required = false) String orderId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "接收状态") @RequestParam(required = false) String receiveStatus,
            @Parameter(description = "起始时间(秒级时间戳)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(秒级时间戳)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {

        PageResult<OrderPushLogDTO> result = orderPushLogService.getPushLogs(
                orderId, status, receiveStatus, startTime, endTime, pageNo, pageSize);

        PageResult<OrderPushLogRespVO> voResult = new PageResult<>();
        voResult.setTotal(result.getTotal());
        voResult.setList(convertToVO(result.getList()));
        return CommonResult.success(voResult);
    }

    private List<OrderPushLogRespVO> convertToVO(List<OrderPushLogDTO> dtos) {
        return dtos.stream().map(dto -> {
            OrderPushLogRespVO vo = new OrderPushLogRespVO();
            vo.setOrderId(dto.getOrderId());
            vo.setChannelOrderId(dto.getChannelOrderId());
            vo.setMerchantCode(dto.getMerchantCode());
            vo.setErpStoreCode(dto.getErpStoreCode());
            vo.setStatus(dto.getStatus());
            vo.setStatusName(dto.getStatusName());
            vo.setTicket(dto.getTicket());
            vo.setPushTime(dto.getPushTime());
            vo.setPushTimeStr(dto.getPushTimeStr());
            vo.setReceiveStatus(dto.getReceiveStatus());
            vo.setConsumeStatus(dto.getConsumeStatus());
            vo.setErrorMessage(dto.getErrorMessage());
            vo.setKafkaPartition(dto.getKafkaPartition());
            vo.setKafkaOffset(dto.getKafkaOffset());
            vo.setWebsocketPushStatus(dto.getWebsocketPushStatus());
            vo.setConsumeTime(dto.getConsumeTime());
            vo.setConsumeTimeStr(dto.getConsumeTimeStr());
            return vo;
        }).toList();
    }
}
