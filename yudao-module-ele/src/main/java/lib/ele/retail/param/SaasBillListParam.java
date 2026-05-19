package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaasBillListParam extends AbstractAPIRequest<SaasBillListResult> {

    private SaasBillListBody body;

    public SaasBillListParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.bill.list", 3);
    }
}
