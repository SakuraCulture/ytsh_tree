package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@TableName("bill_sync_fail_log")
@Data
public class BillSyncFailLogDO {

    @TableId
    private Long id;

    private LocalDate billDate;

    private String merchantCode;

    private String storeCode;

    private String storeName;

    private String marketUserId;

    private Integer failPage;

    private String failReason;

    private Integer retryCount;

    private Integer retryStatus;

    private Date lastRetryTime;

    private Date resolveTime;

    private Date syncTime;

    private Long createTime;

    private Long updateTime;

    private String tenantId;

    private Boolean deleted;
}
