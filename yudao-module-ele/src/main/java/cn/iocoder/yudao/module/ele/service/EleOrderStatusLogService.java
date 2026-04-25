package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderStatusLogPageRespVO;

public interface EleOrderStatusLogService {

    PageResult<EleOrderStatusLogPageRespVO> getStatusLogPage(String orderId, String channelOrderId,
                                                   Long storeId, String changeSource,
                                                   Long startTime, Long endTime,
                                                   Integer pageNo, Integer pageSize);

    EleOrderStatusLogPageRespVO getStatusLogById(Long id);
}