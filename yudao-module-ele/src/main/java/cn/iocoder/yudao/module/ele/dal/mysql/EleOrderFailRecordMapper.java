package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleOrderFailRecordMapper extends BaseMapperX<EleOrderFailRecord> {

    default List<EleOrderFailRecord> selectRetryableList(int maxRetryCount) {
        return selectList(new LambdaQueryWrapperX<EleOrderFailRecord>()
                .in(EleOrderFailRecord::getProcessStatus, "INIT", "FAILED")
                .lt(EleOrderFailRecord::getRetryCount, maxRetryCount)
                .orderByAsc(EleOrderFailRecord::getCreateTime));
    }
}
