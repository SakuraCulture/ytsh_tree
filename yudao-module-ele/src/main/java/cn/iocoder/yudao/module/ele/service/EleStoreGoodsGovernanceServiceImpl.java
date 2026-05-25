package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsGovernancePoolMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleStoreGoodsGovernanceServiceImpl implements EleStoreGoodsGovernanceService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String REMARK_PROCESSED = "已处理";
    private static final String REMARK_IGNORED = "已忽略";

    @Resource
    private EleStoreGoodsGovernancePoolMapper governancePoolMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long create(EleStoreGoodsGovernancePoolDO governancePool) {
        governancePoolMapper.insert(governancePool);
        return governancePool.getId();
    }

    @Override
    public PageResult<EleStoreGoodsGovernancePoolRespVO> getPage(EleStoreGoodsGovernancePoolPageReqVO reqVO) {
        return BeanUtils.toBean(governancePoolMapper.selectPage(reqVO), EleStoreGoodsGovernancePoolRespVO.class);
    }

    @Override
    public EleStoreGoodsGovernancePoolRespVO getById(Long id) {
        EleStoreGoodsGovernancePoolDO governancePool = governancePoolMapper.selectById(id);
        return governancePool == null ? null : BeanUtils.toBean(governancePool, EleStoreGoodsGovernancePoolRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProcessed(Long id, String remark) {
        updateStatus(id, STATUS_PROCESSED, StrUtil.blankToDefault(remark, REMARK_PROCESSED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markIgnored(Long id, String remark) {
        updateStatus(id, STATUS_IGNORED, StrUtil.blankToDefault(remark, REMARK_IGNORED));
    }

    private void updateStatus(Long id, String processStatus, String remark) {
        EleStoreGoodsGovernancePoolDO governancePool = governancePoolMapper.selectById(id);
        if (governancePool == null) {
            throw new RuntimeException("待治理记录不存在");
        }
        if (!STATUS_PENDING.equals(governancePool.getProcessStatus())) {
            throw new RuntimeException("待治理记录不是待处理状态");
        }
        governancePoolMapper.update(new EleStoreGoodsGovernancePoolDO(), new UpdateWrapper<EleStoreGoodsGovernancePoolDO>()
                .eq("id", id)
                .eq("erp_store_code", governancePool.getErpStoreCode())
                .set("process_status", processStatus)
                .set("remark", remark));
    }
}
