package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderFailRecordMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderStatusLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderSyncLogMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderDiscountMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderItemMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderPlatformMapper;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import cn.iocoder.yudao.module.ele.mq.EleOrderKafkaProducer;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.SaasOrderGetResult;
import lib.ele.retail.param.SaasOrderListResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleOrderServiceImplTest extends BaseMockitoUnitTest {

    @Spy
    @InjectMocks
    private EleOrderServiceImpl eleOrderService;

    @Mock
    private EleApiConfigMapper eleApiConfigMapper;
    @Mock
    private cn.iocoder.yudao.module.business.service.store.StoreService storeService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderPlatformMapper orderPlatformMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderDiscountMapper orderDiscountMapper;
    @Mock
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;
    @Mock
    private EleOrderFailRecordMapper eleOrderFailRecordMapper;
    @Mock
    private EleOrderStatusLogMapper eleOrderStatusLogMapper;
    @Mock
    private EleOrderKafkaProducer eleOrderKafkaProducer;
    @Mock
    private EleOrderConvertService eleOrderConvertService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EleOrderLockService eleOrderLockService;
    @Mock
    private EleOpenApiClient eleOpenApiClient;

    @Test
    void saveOrUpdateBatch_shouldSkipNonTerminalStatus() {
        OrderListRespDTO.OrderDetail nonTerminal = buildOrder("order-1", 5);
        OrderListRespDTO.OrderDetail terminal = buildOrder("order-2", 6);

        when(eleOrderLockService.tryLockOrderWithWatchdog("order-2", 1)).thenReturn(true);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(orderPlatformMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.insertOrUpdate(any(OrderDO.class))).thenReturn(true);

        eleOrderService.saveOrUpdateBatch(List.of(nonTerminal, terminal), "store-1", "merchant", "1001");

        verify(orderMapper, times(1)).insertOrUpdate(any(OrderDO.class));
        verify(orderMapper, never()).insertOrUpdate(argThat((OrderDO order) -> "order-1".equals(order.getOrderId())));
    }

    @Test
    void consumeOrderMessage_shouldSkipNonTerminalStatus() {
        OrderMessage nonTerminal = buildMessage("order-3", 4);

        eleOrderService.consumeOrderMessage(nonTerminal);

        verify(orderMapper, never()).insertOrUpdate(any(OrderDO.class));
        verify(orderPlatformMapper, never()).insert(any(cn.iocoder.yudao.module.ele.dal.dataobject.OrderPlatformDO.class));
        verify(orderItemMapper, never()).delete(any(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class));
        verify(orderDiscountMapper, never()).delete(any(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class));
    }

    @Test
    void consumeOrderMessage_shouldPersistTerminalStatus() {
        OrderMessage terminal = buildMessage("order-4", -1);

        when(eleOrderLockService.tryLockOrderWithWatchdog("order-4", 1)).thenReturn(true);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(orderPlatformMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.insertOrUpdate(any(OrderDO.class))).thenReturn(true);

        eleOrderService.consumeOrderMessage(terminal);

        verify(orderMapper).insertOrUpdate(any(OrderDO.class));
    }

    @Test
    void enum_shouldRecognizeTerminalStatus() {
        assertEquals(true, cn.iocoder.yudao.module.ele.enums.EleOrderStatusEnum.isTerminalStatus(6));
        assertEquals(true, cn.iocoder.yudao.module.ele.enums.EleOrderStatusEnum.isTerminalStatus(-1));
        assertEquals(false, cn.iocoder.yudao.module.ele.enums.EleOrderStatusEnum.isTerminalStatus(5));
        assertEquals(false, cn.iocoder.yudao.module.ele.enums.EleOrderStatusEnum.isTerminalStatus(null));
    }

    @Test
    void getOrderList_shouldDelegateToOpenApiClient() {
        cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig config = new cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig();
        config.setMerchantCode("merchant-default");
        when(eleApiConfigMapper.selectActive()).thenReturn(config);

        BizResultWrapper<SaasOrderListResult> wrapper = new BizResultWrapper<>();
        SaasOrderListResult result = new SaasOrderListResult();
        result.setErrno("0");
        wrapper.setBody(result);
        when(eleOpenApiClient.sendOrderList(any(), any(), any(), any(), any())).thenReturn(wrapper);

        OrderListReqDTO req = new OrderListReqDTO();
        req.setMerchantCode("merchant");
        req.setErpStoreCode("1001");
        req.setPageSize(20);

        eleOrderService.getOrderList(req);

        verify(eleOpenApiClient).sendOrderList(any(), any(), any(), any(), any());
    }

    @Test
    void getOrderDetail_shouldDelegateToOpenApiClientWhenLocalMissing() {
        cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig config = new cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig();
        config.setMerchantCode("merchant-default");
        when(eleApiConfigMapper.selectActive()).thenReturn(config);
        when(orderMapper.selectList(any())).thenReturn(List.of());

        BizResultWrapper<SaasOrderGetResult> wrapper = new BizResultWrapper<>();
        SaasOrderGetResult result = new SaasOrderGetResult();
        result.setErrno("0");
        SaasOrderGetResult.SaasOrderGetData data = new SaasOrderGetResult.SaasOrderGetData();
        data.setOrder_id("order-5");
        data.setStatus(5);
        result.setData(data);
        wrapper.setBody(result);
        when(eleOpenApiClient.sendOrderDetail(any(), any(), any(), any(), any(), any())).thenReturn(wrapper);

        OrderDetailRespDTO detail = eleOrderService.getOrderDetail(null, "merchant", "1001", "order-5");

        assertEquals("order-5", detail.getOrderId());
        verify(eleOpenApiClient).sendOrderDetail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void syncOrders_shouldAdvanceWindowFromLastSyncEndTime() {
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setPlatformStoreId("store-1");
        store.setSettlementAccount("merchant");
        store.setStoreId("1001");

        EleOrderSyncLog lastSync = new EleOrderSyncLog();
        lastSync.setLastSyncTime(100L);
        lastSync.setSyncTime(200L);

        when(eleOrderLockService.tryLockSync("store-1", 1, 5)).thenReturn(true);
        when(storeService.getPlatformTableByPlatformStoreId("store-1")).thenReturn(store);
        when(eleOrderSyncLogMapper.selectLastSync("store-1")).thenReturn(lastSync);
        when(eleOrderSyncLogMapper.insert(any(EleOrderSyncLog.class))).thenReturn(1);
        OrderListRespDTO emptyResult = new OrderListRespDTO();
        doReturn(emptyResult).when(eleOrderService).getOrderList(any());

        eleOrderService.syncOrders("store-1", "merchant", "1001");

        verify(eleOrderSyncLogMapper).insert(argThat((EleOrderSyncLog log) -> Long.valueOf(200L).equals(log.getLastSyncTime())));
    }

    @Test
    void retryFailRecord_shouldUseLocalStoreCodeAsErpStoreCode() {
        EleOrderFailRecord record = new EleOrderFailRecord();
        record.setId(1L);
        record.setOrderId("order-retry-1");
        record.setRetryCount(0);

        OrderDO localOrder = new OrderDO();
        localOrder.setOrderId("order-retry-1");
        localOrder.setStoreCode("1001");

        EleApiConfig config = new EleApiConfig();
        config.setMerchantCode("merchant-default");

        OrderDetailRespDTO localDetail = new OrderDetailRespDTO();
        localDetail.setOrderId("order-retry-1");
        localDetail.setStoreCode("1001");

        BizResultWrapper<SaasOrderGetResult> wrapper = new BizResultWrapper<>();
        SaasOrderGetResult result = new SaasOrderGetResult();
        result.setErrno("0");
        SaasOrderGetResult.SaasOrderGetData data = new SaasOrderGetResult.SaasOrderGetData();
        data.setOrder_id("order-retry-1");
        data.setStatus(5);
        data.setStore_code("1001");
        data.setErp_store_code("1001");
        wrapper.setBody(result);
        result.setData(data);

        when(eleOrderFailRecordMapper.selectById(1L)).thenReturn(record);
        when(orderMapper.selectList(any())).thenReturn(List.of(localOrder));
        when(eleOrderConvertService.assembleOrderDetail(localOrder)).thenReturn(localDetail);
        when(eleApiConfigMapper.selectActive()).thenReturn(config);
        when(eleOpenApiClient.sendOrderDetail(any(), any(), eq("merchant-default"), eq(null), eq("1001"), eq("order-retry-1")))
                .thenReturn(wrapper);

        assertDoesNotThrow(() -> eleOrderService.retryFailRecord(1L));

        verify(eleOpenApiClient).sendOrderDetail(any(), any(), eq("merchant-default"), eq(null), eq("1001"), eq("order-retry-1"));
    }

    private static OrderListRespDTO.OrderDetail buildOrder(String orderId, Integer status) {
        OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
        detail.setOrderId(orderId);
        detail.setStatus(status);
        detail.setChannelOrderId(orderId + "-channel");
        detail.setCreateTime(1710000000L);
        detail.setPayTime(1710000100L);
        detail.setBuyerName("buyer");
        detail.setBuyerPhone("13800000000");
        detail.setBuyerAddress("address");
        detail.setStoreCode("1001");
        detail.setErpStoreCode("1001");
        detail.setDeliveryStatus(1);
        return detail;
    }

    private static OrderMessage buildMessage(String orderId, Integer status) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setStatus(status);
        message.setPlatformStoreId("store-1");
        message.setMerchantCode("merchant");
        message.setErpStoreCode("1001");
        message.setChannelOrderId(orderId + "-channel");
        message.setCreateTime(1710000000L);
        message.setPayTime(1710000100L);
        message.setBuyerName("buyer");
        message.setBuyerPhone("13800000000");
        message.setBuyerAddress("address");
        message.setDeliveryStatus(1);
        return message;
    }
}
