package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;

import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;

@Schema(description = "管理后台 - 门店新增/修改 Request VO")
@Data
public class StoreSaveReqVO {

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "17246")
    private String storeId;

    @Schema(description = "门店名称", example = "王五")
    private String storeName;

    @Schema(description = "行政区划代码")
    private String regionCode;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "门店区域(EAST华东/NORTH华北/SOUTH华南/WEST华西/CENTRAL华中)")
    private String area;

    @Schema(description = "状态(0停用1正常)", example = "1")
    private Integer storeStatus;

    @Schema(description = "门店空间")
    private SpaceTableDO spaceTable;

    @Schema(description = "门店架构归属")
    private AffiliationTableDO affiliationTable;

    @Schema(description = "门店经营状态")
    private BusinessStatusTableDO statusTable;

    @Schema(description = "门店加盟商信息")
    private FranchiseeTableDO franchiseeTable;

    @Schema(description = "门店联系人通讯录列表")
    private List<ContactTableDO> contactTables;

}