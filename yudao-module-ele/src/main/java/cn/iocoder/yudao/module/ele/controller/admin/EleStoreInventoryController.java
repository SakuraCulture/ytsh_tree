package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleSkuInventoryBatchQueryReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportExcelVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportRespVO;
import cn.iocoder.yudao.module.ele.service.EleSkuInventoryQueryService;
import cn.iocoder.yudao.module.ele.service.EleStoreInventoryImportService;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "管理后台 - 饿了么门店库存查询")
@RestController
@RequestMapping("/ele/store-inventory")
@Validated
@TenantIgnore
public class EleStoreInventoryController {

    @Resource
    private EleSkuInventoryQueryService eleSkuInventoryQueryService;
    @Resource
    private EleStoreInventoryImportService eleStoreInventoryImportService;

    @GetMapping("/import-template")
    @Operation(summary = "下载门店库存导入模板")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "门店库存导入模板.xls", "库存导入", EleStoreInventoryImportExcelVO.class, List.of());
    }

    @PostMapping("/import")
    @Operation(summary = "导入门店库存")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<EleStoreInventoryImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<EleStoreInventoryImportExcelVO> list = ExcelUtils.read(file, EleStoreInventoryImportExcelVO.class);
        return CommonResult.success(eleStoreInventoryImportService.importRows(list));
    }

    @PostMapping("/query")
    @Operation(summary = "批量查询门店库存并治理")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleSkuInventoryBatchQueryRespDTO> queryStoreInventory(
            @Valid @RequestBody EleSkuInventoryBatchQueryReqVO reqVO) {
        EleSkuInventoryBatchQueryReqBO reqBO = BeanUtils.toBean(reqVO, EleSkuInventoryBatchQueryReqBO.class);
        return CommonResult.success(eleSkuInventoryQueryService.queryBatch(reqBO));
    }
}
