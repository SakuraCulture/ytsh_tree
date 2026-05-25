package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店联系人通讯录 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface ContactTableMapper extends BaseMapperX<ContactTableDO> {

    default List<ContactTableDO> selectListByStoreId(String storeId) {
        return selectList(ContactTableDO::getStoreId, storeId);
    }

    default int deleteByStoreId(String storeId) {
        return delete(ContactTableDO::getStoreId, storeId);
    }

	default int deleteByStoreIds(List<String> storeIds) {
	    return deleteBatch(ContactTableDO::getStoreId, storeIds);
	}

    default List<ContactTableDO> selectListByStoreIds(Collection<String> storeIds) {
        return selectList(new LambdaQueryWrapperX<ContactTableDO>()
                .in(ContactTableDO::getStoreId, storeIds)
                .eq(ContactTableDO::getDeleted, false)
                .orderByAsc(ContactTableDO::getStoreId)
                .orderByAsc(ContactTableDO::getContactId));
    }

}