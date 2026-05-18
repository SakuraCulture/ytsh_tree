package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EleStoreInventoryBatchTaskMapper extends BaseMapperX<EleStoreInventoryBatchTaskDO> {

    List<String> RUNNING_STATUS_LIST = List.of("PENDING", "RUNNING");

    @Select("""
            SELECT t.*
            FROM ele_store_inventory_batch_task t
            INNER JOIN ele_store_inventory_batch_task_store s ON s.task_id = t.id
            WHERE t.scope = 'CURRENT_STORE'
              AND s.erp_store_code = #{erpStoreCode}
              AND t.status IN ('PENDING', 'RUNNING')
            ORDER BY t.create_time DESC
            LIMIT 1
            """)
    EleStoreInventoryBatchTaskDO selectRunningCurrentStore(@Param("erpStoreCode") String erpStoreCode);

    default EleStoreInventoryBatchTaskDO selectRunningAllOpenStores() {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .eq(EleStoreInventoryBatchTaskDO::getScope, "ALL_OPEN_STORES")
                .in(EleStoreInventoryBatchTaskDO::getStatus, RUNNING_STATUS_LIST)
                .orderByDesc(EleStoreInventoryBatchTaskDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default PageResult<EleStoreInventoryBatchTaskDO> selectPage(EleStoreInventoryBatchTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreInventoryBatchTaskDO>()
                .likeIfPresent(EleStoreInventoryBatchTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getScope, reqVO.getScope())
                .eqIfPresent(EleStoreInventoryBatchTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(EleStoreInventoryBatchTaskDO::getCreateTime));
    }
}
