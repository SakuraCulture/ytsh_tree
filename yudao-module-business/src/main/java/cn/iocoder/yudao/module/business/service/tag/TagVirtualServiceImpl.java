package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagVirtualDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagVirtualMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;

@Service
@Validated
public class TagVirtualServiceImpl implements TagVirtualService {

    @Resource
    private TagVirtualMapper tagVirtualMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Long createTagVirtual(TagVirtualSaveReqVO createReqVO) {
        validateDomainType(createReqVO.getDomainType());
        validateExpressionJson(createReqVO.getExpressionJson());
        validateCodeUnique(null, createReqVO.getDomainType(), createReqVO.getCode());
        Integer status = validateStatus(createReqVO.getStatus());

        TagVirtualDO tagVirtual = BeanUtils.toBean(createReqVO, TagVirtualDO.class);
        tagVirtual.setStatus(status);
        tagVirtual.setUniqueDeleted(0L);
        tagVirtualMapper.insert(tagVirtual);
        return tagVirtual.getId();
    }

    @Override
    public void updateTagVirtual(TagVirtualSaveReqVO updateReqVO) {
        validateTagVirtualExists(updateReqVO.getId());
        validateDomainType(updateReqVO.getDomainType());
        validateExpressionJson(updateReqVO.getExpressionJson());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getDomainType(), updateReqVO.getCode());
        Integer status = validateStatus(updateReqVO.getStatus());

        TagVirtualDO updateObj = BeanUtils.toBean(updateReqVO, TagVirtualDO.class);
        updateObj.setStatus(status);
        tagVirtualMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagVirtual(Long id) {
        validateTagVirtualExists(id);

        TagVirtualDO updateObj = new TagVirtualDO();
        updateObj.setId(id);
        updateObj.setUniqueDeleted(id);
        tagVirtualMapper.updateById(updateObj);
        tagVirtualMapper.deleteById(id);
    }

    @Override
    public TagVirtualDO getTagVirtual(Long id) {
        return tagVirtualMapper.selectById(id);
    }

    @Override
    public PageResult<TagVirtualDO> getTagVirtualPage(TagVirtualPageReqVO pageReqVO) {
        return tagVirtualMapper.selectPage(pageReqVO);
    }

    private TagVirtualDO validateTagVirtualExists(Long id) {
        TagVirtualDO tagVirtual = tagVirtualMapper.selectById(id);
        if (tagVirtual == null) {
            throw exception(TAG_VIRTUAL_NOT_EXISTS);
        }
        return tagVirtual;
    }

    private void validateDomainType(String domainType) {
        if (!DOMAIN_TYPES.contains(domainType)) {
            throw exception(TAG_DOMAIN_TYPE_INVALID);
        }
    }

    private void validateExpressionJson(String expressionJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(expressionJson);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw exception(TAG_VIRTUAL_EXPRESSION_INVALID);
            }
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw exception(TAG_VIRTUAL_EXPRESSION_INVALID);
        }
    }

    private void validateCodeUnique(Long id, String domainType, String code) {
        TagVirtualDO tagVirtual = tagVirtualMapper.selectByDomainTypeAndCode(domainType, code);
        if (tagVirtual == null) {
            return;
        }
        if (id == null || !Objects.equals(tagVirtual.getId(), id)) {
            throw exception(TAG_VIRTUAL_CODE_EXISTS);
        }
    }

    private Integer validateStatus(Integer status) {
        if (status == null) {
            return STATUS_ENABLED;
        }
        if (!Objects.equals(status, STATUS_DISABLED) && !Objects.equals(status, STATUS_ENABLED)) {
            throw exception(TAG_VIRTUAL_STATUS_INVALID);
        }
        return status;
    }

}
