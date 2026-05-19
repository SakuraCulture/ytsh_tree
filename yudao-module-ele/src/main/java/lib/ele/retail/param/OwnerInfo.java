package lib.ele.retail.param;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class OwnerInfo {

    @JSONField(name = "ownerCode")
    private String owner_code;
    @JSONField(name = "ownerName")
    private String owner_name;
}
