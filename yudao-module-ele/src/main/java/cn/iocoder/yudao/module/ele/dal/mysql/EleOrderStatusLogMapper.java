package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleOrderStatusLogMapper extends BaseMapperX<EleOrderStatusLog> {

    default List<EleOrderStatusLog> selectByOrderId(String orderId) {
        return selectList(new LambdaQueryWrapperX<EleOrderStatusLog>()
                .eq(EleOrderStatusLog::getOrderId, orderId)
                .orderByAsc(EleOrderStatusLog::getCreateTime));
    }
}
