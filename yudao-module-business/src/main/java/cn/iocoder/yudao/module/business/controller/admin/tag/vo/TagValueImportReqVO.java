package cn.iocoder.yudao.module.business.controller.admin.tag.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagValueImportReqVO {

    @ExcelProperty("对象域")
    private String domainType;

    @ExcelProperty("L1维度名称")
    private String l1Name;

    @ExcelProperty("L1维度编码")
    private String l1Code;

    @ExcelProperty("L2维度名称")
    private String l2Name;

    @ExcelProperty("L2维度编码")
    private String l2Code;

    @ExcelProperty("L3维度名称")
    private String l3Name;

    @ExcelProperty("L3维度编码")
    private String l3Code;

    @ExcelProperty("标签值名称")
    private String tagValueName;

    @ExcelProperty("标签值编码")
    private String tagValueCode;

    @ExcelProperty("打标方式")
    private String tagMethod;

    @ExcelProperty("数据来源")
    private String dataSource;

    @ExcelProperty("更新频率")
    private String updateFrequency;

    @ExcelProperty("逻辑说明")
    private String logicDescription;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("状态")
    private Integer status;

}
