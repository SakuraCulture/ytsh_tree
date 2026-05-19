package lib.ele.retail.param;

import lombok.Data;

@Data
public class SaasBillListBody {
    private Integer status;
    private String merchant_code;
    private String erp_store_code;
    private String market_user_id;
    private String bill_date;
    private Integer page_num;
    private Integer page_size;
}
