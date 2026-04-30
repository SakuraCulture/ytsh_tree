package cn.iocoder.yudao.module.ele.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaasPushMessageDTO {
    
    @JsonProperty("cmd")
    private String cmd;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("ticket")
    private String ticket;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("sign")
    private String sign;
    
    @JsonProperty("encrypt")
    private String encrypt;
    
    @JsonProperty("body")
    private String body;
}
