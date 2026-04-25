package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店加盟商信息 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface FranchiseeTableMapper extends BaseMapperX<FranchiseeTableDO> {

    default FranchiseeTableDO selectByStoreId(String storeId) {
        return selectOne(FranchiseeTableDO::getStoreId, storeId);
    }

    default int deleteByStoreId(String storeId) {
        return delete(FranchiseeTableDO::getStoreId, storeId);
    }

	default int deleteByStoreIds(List<String> storeIds) {
	    return deleteBatch(FranchiseeTableDO::getStoreId, storeIds);
	}

}