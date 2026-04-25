package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店架构归属 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface AffiliationTableMapper extends BaseMapperX<AffiliationTableDO> {

    default AffiliationTableDO selectByStoreId(String storeId) {
        return selectOne(AffiliationTableDO::getStoreId, storeId);
    }

    default int deleteByStoreId(String storeId) {
        return delete(AffiliationTableDO::getStoreId, storeId);
    }

	default int deleteByStoreIds(List<String> storeIds) {
	    return deleteBatch(AffiliationTableDO::getStoreId, storeIds);
	}

}