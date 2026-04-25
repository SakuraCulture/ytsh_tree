package cn.iocoder.yudao.module.ele.service.client;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.integration.apilog.service.ExternalApiCallLogService;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.SaasOrderListParam;
import lib.ele.retail.param.SaasOrderListResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EleOpenApiClientImplTest extends BaseMockitoUnitTest {

    @Spy
    @InjectMocks
    private EleOpenApiClientImpl client;

    @Mock
    private ExternalApiCallLogService externalApiCallLogService;

    @Mock
    private ApiExecutor<SaasOrderListResult> listExecutor;

    @Test
    void sendOrderList_shouldSkipExternalApiCallLogWhenSuccess() throws Exception {
        EleApiConfig config = new EleApiConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");

        SaasOrderListParam param = new SaasOrderListParam();
        param.setTicket("ticket-1");

        SaasOrderListResult body = new SaasOrderListResult();
        body.setErrno("0");
        body.setError("ok");
        BizResultWrapper<SaasOrderListResult> wrapper = new BizResultWrapper<>();
        wrapper.setTraceid("ext-trace-1");
        wrapper.setBody(body);

        doReturn(listExecutor).when(client).buildListExecutor(config);
        doReturn(wrapper).when(listExecutor).send(param);

        client.sendOrderList(config, param, "merchant", "store-1", "1001");

        verify(externalApiCallLogService, never()).createExternalApiCallLog(any());
    }

    @Test
    void sendOrderList_shouldRecordExternalApiCallLogWhenSuccessLoggingEnabled() throws Exception {
        ReflectionTestUtils.setField(client, "logSuccessEnabled", true);

        EleApiConfig config = new EleApiConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");

        SaasOrderListParam param = new SaasOrderListParam();
        param.setTicket("ticket-1");

        SaasOrderListResult body = new SaasOrderListResult();
        body.setErrno("0");
        body.setError("ok");
        BizResultWrapper<SaasOrderListResult> wrapper = new BizResultWrapper<>();
        wrapper.setTraceid("ext-trace-1");
        wrapper.setBody(body);

        doReturn(listExecutor).when(client).buildListExecutor(config);
        doReturn(wrapper).when(listExecutor).send(param);

        client.sendOrderList(config, param, "merchant", "store-1", "1001");

        ArgumentCaptor<ExternalApiCallLogCreateReqBO> captor = ArgumentCaptor.forClass(ExternalApiCallLogCreateReqBO.class);
        verify(externalApiCallLogService).createExternalApiCallLog(captor.capture());
        assertEquals(true, captor.getValue().getSuccess());
        assertEquals("0", captor.getValue().getResultCode());
    }

    @Test
    void sendOrderList_shouldRecordExternalApiCallLogWhenBizFailed() throws Exception {
        EleApiConfig config = new EleApiConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");

        SaasOrderListParam param = new SaasOrderListParam();
        param.setTicket("ticket-1");

        SaasOrderListResult body = new SaasOrderListResult();
        body.setErrno("50001");
        body.setError("biz failed");
        BizResultWrapper<SaasOrderListResult> wrapper = new BizResultWrapper<>();
        wrapper.setTraceid("ext-trace-1");
        wrapper.setBody(body);

        doReturn(listExecutor).when(client).buildListExecutor(config);
        doReturn(wrapper).when(listExecutor).send(param);

        client.sendOrderList(config, param, "merchant", "store-1", "1001");

        ArgumentCaptor<ExternalApiCallLogCreateReqBO> captor = ArgumentCaptor.forClass(ExternalApiCallLogCreateReqBO.class);
        verify(externalApiCallLogService).createExternalApiCallLog(captor.capture());
        assertEquals("ELE", captor.getValue().getPlatformCode());
        assertEquals("ORDER_LIST", captor.getValue().getApiCode());
        assertEquals("ext-trace-1", captor.getValue().getExternalTraceId());
        assertEquals("merchant", captor.getValue().getMerchantCode());
        assertEquals(false, captor.getValue().getSuccess());
        assertEquals("50001", captor.getValue().getResultCode());
    }

    @Test
    void sendOrderList_shouldRecordExternalApiCallLogWhenExceptionThrown() throws Exception {
        EleApiConfig config = new EleApiConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");

        SaasOrderListParam param = new SaasOrderListParam();
        param.setTicket("ticket-1");

        doReturn(listExecutor).when(client).buildListExecutor(config);
        doThrow(new RuntimeException("network error")).when(listExecutor).send(param);

        assertThrows(RuntimeException.class, () -> client.sendOrderList(config, param, "merchant", "store-1", "1001"));

        ArgumentCaptor<ExternalApiCallLogCreateReqBO> captor = ArgumentCaptor.forClass(ExternalApiCallLogCreateReqBO.class);
        verify(externalApiCallLogService).createExternalApiCallLog(captor.capture());
        assertEquals(false, captor.getValue().getSuccess());
        assertEquals("network error", captor.getValue().getResultMsg());
    }
}
