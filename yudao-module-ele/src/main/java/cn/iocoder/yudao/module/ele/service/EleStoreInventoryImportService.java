package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportExcelVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportRespVO;

import java.util.List;

public interface EleStoreInventoryImportService {

    EleStoreInventoryImportRespVO importRows(List<EleStoreInventoryImportExcelVO> rows);
}
