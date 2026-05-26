package lib.ele.retail.param;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class MeEleRetailSaasOrderListResDto {

    private String scroll_id;
    private MeEleRetailSaasOrderListDetailResDto[] order_list;
    private Long total;

    
    public String getScroll_id() {
        return scroll_id;
    }

    
    public void setScroll_id(String scroll_id) {
        this.scroll_id = scroll_id;
    }

    
    public MeEleRetailSaasOrderListDetailResDto[] getOrder_list() {
        return order_list;
    }

    
    public void setOrder_list(MeEleRetailSaasOrderListDetailResDto[] order_list) {
        this.order_list = order_list;
    }

    
    public Long getTotal() {
        return total;
    }

    
    public void setTotal(Long total) {
        this.total = total;
    }

}
