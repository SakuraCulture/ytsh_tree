package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("ele_api_config")
@Data
public class EleApiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;

    private String appSecret;

    private String developerId;

    private String token;

    private String encryptType;

    private String apiUrl;

    private String apiVersion;

    private String accessToken;

    private String merchantCode;

    private String apiName;

    private String apiCmd;

    private String apiDescription;

    private String bodyTemplate;

    private String encrypt;

    private String source;

    private Integer status;

    private String remark;

    private Long createTime;

    private Long updateTime;

    private String createBy;

    private String updateBy;
}
