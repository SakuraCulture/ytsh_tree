package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店经营状态 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface BusinessStatusTableMapper extends BaseMapperX<BusinessStatusTableDO> {

    default BusinessStatusTableDO selectByStoreId(String storeId) {
        return selectOne(BusinessStatusTableDO::getStoreId, storeId);
    }

    default int deleteByStoreId(String storeId) {
        return delete(BusinessStatusTableDO::getStoreId, storeId);
    }

	default int deleteByStoreIds(List<String> storeIds) {
	    return deleteBatch(BusinessStatusTableDO::getStoreId, storeIds);
	}

}