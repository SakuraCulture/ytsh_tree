package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderTrackingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleOrderTrackingMapper extends BaseMapperX<EleOrderTrackingDO> {

    default List<EleOrderTrackingDO> selectTrackingOrders() {
        return selectList(new LambdaQueryWrapperX<EleOrderTrackingDO>()
                .eq(EleOrderTrackingDO::getTrackingStatus, "TRACKING")
                .orderByAsc(EleOrderTrackingDO::getOrderCreateTime));
    }

    default EleOrderTrackingDO selectByOrderId(String orderId) {
        return selectOne(EleOrderTrackingDO::getOrderId, orderId);
    }
}
