package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.controller.admin.order.vo.OrderListReqVO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;

public interface OrderQueryService {

    OrderListRespDTO getOrderList(OrderListReqVO reqVO);
}
