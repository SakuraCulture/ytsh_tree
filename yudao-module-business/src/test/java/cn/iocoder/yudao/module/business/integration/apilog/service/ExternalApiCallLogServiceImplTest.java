package cn.iocoder.yudao.module.business.integration.apilog.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo.ExternalApiCallLogPageReqVO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.dataobject.ExternalApiCallLogDO;
import cn.iocoder.yudao.module.business.integration.apilog.dal.mysql.ExternalApiCallLogMapper;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(ExternalApiCallLogServiceImpl.class)
class ExternalApiCallLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ExternalApiCallLogService externalApiCallLogService;

    @Resource
    private ExternalApiCallLogMapper externalApiCallLogMapper;

    @Test
    void createExternalApiCallLog_shouldPersistRecord() {
        ExternalApiCallLogCreateReqBO reqBO = new ExternalApiCallLogCreateReqBO();
        reqBO.setPlatformCode("ELE");
        reqBO.setApiCode("ORDER_LIST");
        reqBO.setApiName("订单列表");
        reqBO.setBizType("ORDER");
        reqBO.setBizId("order-1");
        reqBO.setBizNo("channel-1");
        reqBO.setTraceId("trace-1");
        reqBO.setExternalTraceId("ext-trace-1");
        reqBO.setRequestId("req-1");
        reqBO.setRequestMethod("POST");
        reqBO.setRequestBody("{\"page\":1}");
        reqBO.setResponseBody("{\"errno\":\"0\"}");
        reqBO.setSuccess(true);
        reqBO.setResultCode("0");
        reqBO.setResultMsg("ok");
        reqBO.setDurationMs(123);
        reqBO.setMerchantCode("merchant");
        reqBO.setPlatformStoreId("store-1");
        reqBO.setErpStoreCode("1001");
        reqBO.setOrderId("order-1");
        reqBO.setChannelOrderId("channel-1");

        Long id = externalApiCallLogService.createExternalApiCallLog(reqBO);

        ExternalApiCallLogDO log = externalApiCallLogMapper.selectById(id);
        assertEquals("ELE", log.getPlatformCode());
        assertEquals("ORDER_LIST", log.getApiCode());
        assertEquals("ext-trace-1", log.getExternalTraceId());
        assertEquals(true, log.getSuccess());
        AssertUtils.assertPojoEquals(reqBO, log, "id", "createTime", "updateTime", "creator", "updater", "deleted");
    }

    @Test
    void getExternalApiCallLogPage_shouldFilterByExternalTraceId() {
        externalApiCallLogMapper.insert(buildLog("ext-trace-1", "order-1"));
        externalApiCallLogMapper.insert(buildLog("ext-trace-2", "order-2"));

        ExternalApiCallLogPageReqVO reqVO = new ExternalApiCallLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setPlatformCode("ELE");
        reqVO.setExternalTraceId("ext-trace-1");

        var page = externalApiCallLogService.getExternalApiCallLogPage(reqVO);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("order-1", page.getList().get(0).getOrderId());
    }

    private static ExternalApiCallLogDO buildLog(String externalTraceId, String orderId) {
        ExternalApiCallLogDO log = new ExternalApiCallLogDO();
        log.setPlatformCode("ELE");
        log.setApiCode("ORDER_DETAIL");
        log.setApiName("订单详情");
        log.setBizType("ORDER");
        log.setBizId(orderId);
        log.setBizNo(orderId + "-biz");
        log.setTraceId("trace-" + orderId);
        log.setExternalTraceId(externalTraceId);
        log.setRequestId("req-" + orderId);
        log.setRequestMethod("POST");
        log.setSuccess(true);
        log.setResultCode("0");
        log.setResultMsg("ok");
        log.setDurationMs(100);
        log.setMerchantCode("merchant");
        log.setPlatformStoreId("store-1");
        log.setErpStoreCode("1001");
        log.setOrderId(orderId);
        log.setChannelOrderId(orderId + "-channel");
        log.setDeleted(false);
        return log;
    }
}
