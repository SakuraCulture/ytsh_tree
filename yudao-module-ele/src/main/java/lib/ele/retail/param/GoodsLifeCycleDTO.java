package lib.ele.retail.param;

import lombok.Data;

@Data
public class GoodsLifeCycleDTO {

    private String production_date;
    private String expire_date;
    private Integer shelf_life;
}
