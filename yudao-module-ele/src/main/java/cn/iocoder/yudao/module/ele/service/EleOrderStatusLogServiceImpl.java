package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderStatusLogPageRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderStatusLog;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderStatusLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class EleOrderStatusLogServiceImpl implements EleOrderStatusLogService {

    @Resource
    private EleOrderStatusLogMapper eleOrderStatusLogMapper;

    @Override
    public PageResult<EleOrderStatusLogPageRespVO> getStatusLogPage(String orderId, String channelOrderId,
                                                                   Long storeId, String changeSource,
                                                                   Long startTime, Long endTime,
                                                                   Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);

        LambdaQueryWrapperX<EleOrderStatusLog> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(EleOrderStatusLog::getOrderId, orderId)
                .likeIfPresent(EleOrderStatusLog::getChannelOrderId, channelOrderId)
                .eqIfPresent(EleOrderStatusLog::getStoreId, storeId)
                .eqIfPresent(EleOrderStatusLog::getChangeSource, changeSource)
                .geIfPresent(EleOrderStatusLog::getCreateTime, startTime)
                .leIfPresent(EleOrderStatusLog::getCreateTime, endTime)
                .orderByDesc(EleOrderStatusLog::getCreateTime);

        return BeanUtils.toBean(eleOrderStatusLogMapper.selectPage(pageParam, wrapper), EleOrderStatusLogPageRespVO.class);
    }

    @Override
    public EleOrderStatusLogPageRespVO getStatusLogById(Long id) {
        EleOrderStatusLog statusLog = eleOrderStatusLogMapper.selectById(id);
        if (statusLog == null) {
            return null;
        }
        return BeanUtils.toBean(statusLog, EleOrderStatusLogPageRespVO.class);
    }
}