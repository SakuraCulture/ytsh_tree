package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreImportExcelVO {

    @ExcelProperty("门店编码")
    private String storeId;

    @ExcelProperty("门店名称")
    private String storeName;

    @ExcelProperty("行政区划代码")
    private String regionCode;

    @ExcelProperty("详细地址")
    private String address;

    @ExcelProperty("门店区域")
    private String area;

    @ExcelProperty("状态(0停用1正常)")
    private Integer storeStatus;

    @ExcelProperty("房屋面积(㎡)")
    private BigDecimal buildingArea;

    @ExcelProperty("冷库面积(㎡)")
    private BigDecimal coldStorageArea;

    @ExcelProperty("经营方式")
    private String businessMode;

    @ExcelProperty("门店类型")
    private String storeType;

    @ExcelProperty("当前状态")
    private String currentStatus;

    @ExcelProperty("开业日期")
    private LocalDate openDate;

    @ExcelProperty("签约日期")
    private LocalDate signDate;

    @ExcelProperty("加盟商名称")
    private String franchiseeName;

    @ExcelProperty("加盟联系方式")
    private String franchiseePhone;

    @ExcelProperty("加盟费")
    private BigDecimal franchiseeFee;

    @ExcelProperty("保证金")
    private BigDecimal securityDeposit;

    @ExcelProperty("合同开始日期")
    private LocalDate contractStart;

    @ExcelProperty("合同结束日期")
    private LocalDate contractEnd;

}
