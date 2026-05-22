package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsSyncLogRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsSyncLogDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsSyncLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleStoreGoodsSyncLogServiceImpl implements EleStoreGoodsSyncLogService {

    @Resource
    private EleStoreGoodsSyncLogMapper syncLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long create(EleStoreGoodsSyncLogDO syncLog) {
        syncLogMapper.insert(syncLog);
        return syncLog.getId();
    }

    @Override
    public PageResult<EleStoreGoodsSyncLogRespVO> getSyncLogPage(String platformStoreId, String erpStoreCode,
                                                                 String skuCode, Boolean success,
                                                                 Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);
        return BeanUtils.toBean(syncLogMapper.selectPage(platformStoreId, erpStoreCode, skuCode, success, pageParam),
                EleStoreGoodsSyncLogRespVO.class);
    }

    @Override
    public EleStoreGoodsSyncLogRespVO getSyncLogById(Long id) {
        EleStoreGoodsSyncLogDO syncLog = syncLogMapper.selectById(id);
        return syncLog == null ? null : BeanUtils.toBean(syncLog, EleStoreGoodsSyncLogRespVO.class);
    }
}
