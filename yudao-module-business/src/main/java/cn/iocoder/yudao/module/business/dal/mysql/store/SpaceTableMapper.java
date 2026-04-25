package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店空间 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface SpaceTableMapper extends BaseMapperX<SpaceTableDO> {

    default SpaceTableDO selectByStoreId(String storeId) {
        return selectOne(SpaceTableDO::getStoreId, storeId);
    }

    default int deleteByStoreId(String storeId) {
        return delete(SpaceTableDO::getStoreId, storeId);
    }

	default int deleteByStoreIds(List<String> storeIds) {
	    return deleteBatch(SpaceTableDO::getStoreId, storeIds);
	}

}