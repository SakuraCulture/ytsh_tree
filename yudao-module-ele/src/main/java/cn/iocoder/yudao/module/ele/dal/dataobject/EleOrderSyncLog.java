package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("ele_order_sync_log")
@Data
public class EleOrderSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformStoreId;

    private String merchantCode;

    private String erpStoreCode;

    private String storeName;

    private Long lastSyncTime;

    private LocalDateTime syncStartTime;

    private LocalDateTime syncEndTime;

    private Long syncTime;

    private Integer syncCount;

    private Integer successCount;

    private Integer failCount;

    private Integer status;

    private String errorMsg;

    private Long createTime;
}