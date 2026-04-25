package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsGovernancePoolMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsGovernanceServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreGoodsGovernanceServiceImpl governanceService;

    @Mock
    private EleStoreGoodsGovernancePoolMapper governancePoolMapper;

    @Test
    void getPage_shouldReturnMappedPageResult() {
        EleStoreGoodsGovernancePoolPageReqVO reqVO = new EleStoreGoodsGovernancePoolPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSkuCode("SKU001");
        EleStoreGoodsGovernancePoolDO record = new EleStoreGoodsGovernancePoolDO();
        record.setId(1L);
        record.setSkuCode("SKU001");
        record.setProcessStatus("PENDING");
        when(governancePoolMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(record), 1L));

        PageResult<EleStoreGoodsGovernancePoolRespVO> page = governanceService.getPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals("SKU001", page.getList().get(0).getSkuCode());
        verify(governancePoolMapper).selectPage(reqVO);
    }

    @Test
    void markProcessed_shouldUpdateStatusAndRemark() {
        EleStoreGoodsGovernancePoolDO record = new EleStoreGoodsGovernancePoolDO();
        record.setId(1L);
        record.setProcessStatus("PENDING");
        when(governancePoolMapper.selectById(1L)).thenReturn(record);

        governanceService.markProcessed(1L, null);

        verify(governancePoolMapper).updateById(org.mockito.ArgumentMatchers.<EleStoreGoodsGovernancePoolDO>argThat(update ->
                Long.valueOf(1L).equals(update.getId())
                        && "PROCESSED".equals(update.getProcessStatus())
                        && "已处理".equals(update.getRemark())));
    }

    @Test
    void markIgnored_shouldUpdateStatusAndRemark() {
        EleStoreGoodsGovernancePoolDO record = new EleStoreGoodsGovernancePoolDO();
        record.setId(1L);
        record.setProcessStatus("PENDING");
        when(governancePoolMapper.selectById(1L)).thenReturn(record);

        governanceService.markIgnored(1L, null);

        verify(governancePoolMapper).updateById(org.mockito.ArgumentMatchers.<EleStoreGoodsGovernancePoolDO>argThat(update ->
                Long.valueOf(1L).equals(update.getId())
                        && "IGNORED".equals(update.getProcessStatus())
                        && "已忽略".equals(update.getRemark())));
    }

    @Test
    void markProcessed_shouldRejectNonPendingRecord() {
        EleStoreGoodsGovernancePoolDO record = new EleStoreGoodsGovernancePoolDO();
        record.setId(1L);
        record.setProcessStatus("PROCESSED");
        when(governancePoolMapper.selectById(1L)).thenReturn(record);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> governanceService.markProcessed(1L, null));

        assertEquals("待治理记录不是待处理状态", exception.getMessage());
    }
}

