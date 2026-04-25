package cn.iocoder.yudao.module.business.controller.admin.tag;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionListReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.service.tag.TagDimensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 标签维度")
@RestController
@RequestMapping("/business/tag-dimension")
@Validated
public class TagDimensionController {

    @Resource
    private TagDimensionService tagDimensionService;

    @PostMapping("/create")
    @Operation(summary = "创建标签维度")
    @PreAuthorize("@ss.hasPermission('business:tag-dimension:create')")
    public CommonResult<Long> createTagDimension(@Valid @RequestBody TagDimensionSaveReqVO createReqVO) {
        return success(tagDimensionService.createTagDimension(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新标签维度")
    @PreAuthorize("@ss.hasPermission('business:tag-dimension:update')")
    public CommonResult<Boolean> updateTagDimension(@Valid @RequestBody TagDimensionSaveReqVO updateReqVO) {
        tagDimensionService.updateTagDimension(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除标签维度")
    @Parameter(name = "id", description = "标签维度编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:tag-dimension:delete')")
    public CommonResult<Boolean> deleteTagDimension(@RequestParam("id") Long id) {
        tagDimensionService.deleteTagDimension(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得标签维度")
    @Parameter(name = "id", description = "标签维度编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:tag-dimension:query')")
    public CommonResult<TagDimensionRespVO> getTagDimension(@RequestParam("id") Long id) {
        TagDimensionDO tagDimension = tagDimensionService.getTagDimension(id);
        return success(BeanUtils.toBean(tagDimension, TagDimensionRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得标签维度列表")
    @PreAuthorize("@ss.hasPermission('business:tag-dimension:query')")
    public CommonResult<List<TagDimensionRespVO>> getTagDimensionList(@Valid TagDimensionListReqVO listReqVO) {
        List<TagDimensionDO> list = tagDimensionService.getTagDimensionList(
                listReqVO.getDomainType(), listReqVO.getParentId(), listReqVO.getLevel());
        return success(BeanUtils.toBean(list, TagDimensionRespVO.class));
    }

}
