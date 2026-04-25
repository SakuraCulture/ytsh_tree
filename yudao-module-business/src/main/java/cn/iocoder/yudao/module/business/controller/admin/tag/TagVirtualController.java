package cn.iocoder.yudao.module.business.controller.admin.tag;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagVirtualDO;
import cn.iocoder.yudao.module.business.service.tag.TagVirtualService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 虚拟标签")
@RestController
@RequestMapping("/business/tag-virtual")
@Validated
public class TagVirtualController {

    @Resource
    private TagVirtualService tagVirtualService;

    @PostMapping("/create")
    @Operation(summary = "创建虚拟标签")
    @PreAuthorize("@ss.hasPermission('business:tag-virtual:create')")
    public CommonResult<Long> createTagVirtual(@Valid @RequestBody TagVirtualSaveReqVO createReqVO) {
        return success(tagVirtualService.createTagVirtual(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新虚拟标签")
    @PreAuthorize("@ss.hasPermission('business:tag-virtual:update')")
    public CommonResult<Boolean> updateTagVirtual(@Valid @RequestBody TagVirtualSaveReqVO updateReqVO) {
        tagVirtualService.updateTagVirtual(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除虚拟标签")
    @Parameter(name = "id", description = "虚拟标签编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:tag-virtual:delete')")
    public CommonResult<Boolean> deleteTagVirtual(@RequestParam("id") Long id) {
        tagVirtualService.deleteTagVirtual(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得虚拟标签")
    @Parameter(name = "id", description = "虚拟标签编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:tag-virtual:query')")
    public CommonResult<TagVirtualRespVO> getTagVirtual(@RequestParam("id") Long id) {
        TagVirtualDO tagVirtual = tagVirtualService.getTagVirtual(id);
        return success(BeanUtils.toBean(tagVirtual, TagVirtualRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得虚拟标签分页")
    @PreAuthorize("@ss.hasPermission('business:tag-virtual:query')")
    public CommonResult<PageResult<TagVirtualRespVO>> getTagVirtualPage(@Valid TagVirtualPageReqVO pageReqVO) {
        PageResult<TagVirtualDO> pageResult = tagVirtualService.getTagVirtualPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TagVirtualRespVO.class));
    }

}
