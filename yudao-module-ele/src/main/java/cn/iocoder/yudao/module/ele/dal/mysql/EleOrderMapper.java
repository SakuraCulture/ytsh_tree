package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleOrderMapper extends BaseMapperX<EleOrder> {

    default List<EleOrder> selectByPlatformStoreId(String platformStoreId) {
        return selectList(new LambdaQueryWrapperX<EleOrder>()
                .eq(EleOrder::getPlatformStoreId, platformStoreId)
                .orderByDesc(EleOrder::getCreateTime));
    }

    default List<EleOrder> selectByErpStoreCode(String erpStoreCode) {
        return selectList(new LambdaQueryWrapperX<EleOrder>()
                .eq(EleOrder::getErpStoreCode, erpStoreCode)
                .orderByDesc(EleOrder::getCreateTime));
    }

    default EleOrder selectByOrderId(String orderId) {
        List<EleOrder> list = selectList(new LambdaQueryWrapperX<EleOrder>()
                .eq(EleOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    default Long selectCountByPlatformStoreId(String platformStoreId) {
        return selectCount(new LambdaQueryWrapperX<EleOrder>()
                .eq(EleOrder::getPlatformStoreId, platformStoreId));
    }
}