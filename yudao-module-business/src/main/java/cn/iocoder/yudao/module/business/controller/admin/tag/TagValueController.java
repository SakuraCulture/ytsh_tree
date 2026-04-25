package cn.iocoder.yudao.module.business.controller.admin.tag;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValuePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.service.tag.TagValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 标签值")
@RestController
@RequestMapping("/business/tag-value")
@Validated
public class TagValueController {

    @Resource
    private TagValueService tagValueService;

    @PostMapping("/create")
    @Operation(summary = "创建标签值")
    @PreAuthorize("@ss.hasPermission('business:tag-value:create')")
    public CommonResult<Long> createTagValue(@Valid @RequestBody TagValueSaveReqVO createReqVO) {
        return success(tagValueService.createTagValue(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新标签值")
    @PreAuthorize("@ss.hasPermission('business:tag-value:update')")
    public CommonResult<Boolean> updateTagValue(@Valid @RequestBody TagValueSaveReqVO updateReqVO) {
        tagValueService.updateTagValue(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除标签值")
    @Parameter(name = "id", description = "标签值编号", required = true)
    @PreAuthorize("@ss.hasPermission('business:tag-value:delete')")
    public CommonResult<Boolean> deleteTagValue(@RequestParam("id") Long id) {
        tagValueService.deleteTagValue(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得标签值")
    @Parameter(name = "id", description = "标签值编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:tag-value:query')")
    public CommonResult<TagValueRespVO> getTagValue(@RequestParam("id") Long id) {
        TagValueDO tagValue = tagValueService.getTagValue(id);
        return success(BeanUtils.toBean(tagValue, TagValueRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得标签值分页")
    @PreAuthorize("@ss.hasPermission('business:tag-value:query')")
    public CommonResult<PageResult<TagValueRespVO>> getTagValuePage(@Valid TagValuePageReqVO pageReqVO) {
        PageResult<TagValueDO> pageResult = tagValueService.getTagValuePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TagValueRespVO.class));
    }

    @GetMapping("/list-by-dimension")
    @Operation(summary = "根据标签维度获得标签值列表")
    @Parameter(name = "dimensionId", description = "标签维度编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('business:tag-value:query')")
    public CommonResult<List<TagValueRespVO>> getTagValueListByDimension(@RequestParam("dimensionId") Long dimensionId) {
        List<TagValueDO> list = tagValueService.getTagValueListByDimensionId(dimensionId);
        return success(BeanUtils.toBean(list, TagValueRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入标签值模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<TagValueImportReqVO> list = List.of(
                TagValueImportReqVO.builder()
                        .domainType("PRODUCT")
                        .l1Name("运营管理领域")
                        .l1Code("operation")
                        .l2Name("商品角色")
                        .l2Code("product_role")
                        .l3Name("功能角色")
                        .l3Code("function_role")
                        .tagValueName("高价值商品")
                        .tagValueCode("high_value_product")
                        .tagMethod("RULE")
                        .dataSource("订单中心")
                        .updateFrequency("每日")
                        .logicDescription("近30天销售额大于1000")
                        .sort(1)
                        .status(1)
                        .build()
        );
        ExcelUtils.write(response, "标签值导入模板.xls", "标签值列表", TagValueImportReqVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入标签值")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "file", description = "Excel 文件", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('business:tag-value:import')")
    public CommonResult<TagValueImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<TagValueImportReqVO> list = ExcelUtils.read(file, TagValueImportReqVO.class);
        return success(tagValueService.importTagValueList(list, updateSupport));
    }

}
