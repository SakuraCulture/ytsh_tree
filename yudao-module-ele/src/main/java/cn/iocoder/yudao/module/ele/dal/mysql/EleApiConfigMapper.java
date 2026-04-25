package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EleApiConfigMapper extends BaseMapperX<EleApiConfig> {

    default EleApiConfig selectActive() {
        List<EleApiConfig> list = selectList(new LambdaQueryWrapperX<EleApiConfig>()
                .eq(EleApiConfig::getStatus, 1)
                .orderByDesc(EleApiConfig::getId)
                .last("LIMIT 1"));
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }
}