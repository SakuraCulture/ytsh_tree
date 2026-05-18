package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;

import java.util.Collection;
import java.util.List;

public interface TagObjectRelationService {

    void saveManualRelations(String domainType, String objectType, String objectId, List<Long> tagValueIds);

    void saveRuleRelations(String domainType, String objectType, String objectId, String sourceRef, List<Long> tagValueIds);

    List<TagObjectRelationDO> getActiveRelations(String domainType, String objectType, String objectId);

    List<TagObjectRelationDO> getActiveRelationsByObjectIds(String domainType, String objectType, Collection<String> objectIds);

}
