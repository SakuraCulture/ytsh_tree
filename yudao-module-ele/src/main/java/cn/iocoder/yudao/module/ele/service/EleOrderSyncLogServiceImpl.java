package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncLogRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncStatsRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderSyncLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EleOrderSyncLogServiceImpl implements EleOrderSyncLogService {

    @Resource
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;

    @Resource
    private StoreService storeService;

    @Override
    public PageResult<EleOrderSyncLogRespVO> getSyncLogPage(String platformStoreId, String erpStoreCode,
            Integer status, Long startTime, Long endTime,
            Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);

        LambdaQueryWrapperX<EleOrderSyncLog> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(EleOrderSyncLog::getPlatformStoreId, platformStoreId)
                .likeIfPresent(EleOrderSyncLog::getErpStoreCode, erpStoreCode)
                .eqIfPresent(EleOrderSyncLog::getStatus, status)
                .geIfPresent(EleOrderSyncLog::getSyncTime, startTime)
                .leIfPresent(EleOrderSyncLog::getSyncTime, endTime)
                .orderByDesc(EleOrderSyncLog::getSyncTime);

        PageResult<EleOrderSyncLog> pageResult = eleOrderSyncLogMapper.selectPage(pageParam, wrapper);

        List<String> platformStoreIds = pageResult.getList().stream()
                .map(EleOrderSyncLog::getPlatformStoreId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> storeNameMap = platformStoreIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            try {
                                StorePlatformRespVO storeInfo = storeService.getPlatformTableByPlatformStoreId(id);
                                return storeInfo != null ? storeInfo.getPlatformStoreName() : null;
                            } catch (Exception e) {
                                log.warn("[同步日志] 查询门店名称失败, platformStoreId: {}", id, e);
                                return null;
                            }
                        }));

        List<EleOrderSyncLogRespVO> respList = pageResult.getList().stream().map(syncLog -> {
            EleOrderSyncLogRespVO respVO = BeanUtils.toBean(syncLog, EleOrderSyncLogRespVO.class);

            if (syncLog.getSyncStartTime() != null) {
                respVO.setSyncStartTime(syncLog.getSyncStartTime()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            if (syncLog.getSyncEndTime() != null) {
                respVO.setSyncEndTime(syncLog.getSyncEndTime()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            respVO.setSuccessCount(syncLog.getSuccessCount() != null ? syncLog.getSuccessCount() : 0);
            respVO.setFailCount(syncLog.getFailCount() != null ? syncLog.getFailCount() : 0);

            if (syncLog.getPlatformStoreId() != null) {
                respVO.setStoreName(storeNameMap.get(syncLog.getPlatformStoreId()));
            }

            return respVO;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public EleOrderSyncLogRespVO getSyncLogById(Long id) {
        EleOrderSyncLog syncLog = eleOrderSyncLogMapper.selectById(id);
        if (syncLog == null) {
            return null;
        }
        return BeanUtils.toBean(syncLog, EleOrderSyncLogRespVO.class);
    }

    @Override
    public EleOrderSyncStatsRespVO getStoreSyncStats(String platformStoreId) {
        EleOrderSyncStatsRespVO stats = new EleOrderSyncStatsRespVO();
        stats.setPlatformStoreId(platformStoreId);

        Map<String, Object> baseStats = eleOrderSyncLogMapper.selectStoreStats(platformStoreId);
        if (baseStats == null || baseStats.isEmpty()) {
            stats.setTotalSyncCount(0);
            stats.setSuccessCount(0);
            stats.setFailCount(0);
            stats.setSuccessRate(0.0);
            stats.setAvgDuration(0.0);
            stats.setLastSyncTime(null);
            return stats;
        }

        int totalSyncCount = ((Number) baseStats.get("totalSyncCount")).intValue();
        int successCount = ((Number) baseStats.getOrDefault("successCount", 0)).intValue();
        int failCount = ((Number) baseStats.getOrDefault("failCount", 0)).intValue();

        stats.setTotalSyncCount(totalSyncCount);
        stats.setSuccessCount(successCount);
        stats.setFailCount(failCount);
        stats.setSuccessRate(totalSyncCount > 0 ? Math.round((double) successCount / totalSyncCount * 10000) / 100.0 : 0.0);

        Object lastSyncTimeObj = baseStats.get("lastSyncTime");
        if (lastSyncTimeObj != null) {
            stats.setLastSyncTime(((Number) lastSyncTimeObj).longValue());
        }

        Double avgDuration = eleOrderSyncLogMapper.selectAvgDuration(platformStoreId);
        stats.setAvgDuration(avgDuration != null ? Math.round(avgDuration * 10) / 10.0 : 0.0);

        try {
            cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO storeInfo =
                    storeService.getPlatformTableByPlatformStoreId(platformStoreId);
            if (storeInfo != null) {
                stats.setStoreName(storeInfo.getPlatformStoreName());
            }
        } catch (Exception e) {
            log.warn("[同步统计] 查询门店名称失败, platformStoreId: {}", platformStoreId, e);
        }

        return stats;
    }
}