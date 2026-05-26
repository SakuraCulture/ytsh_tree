package lib.ele.retail.param;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class MeEleRetailSaasOrderListReqDto {

    private String market_user_id;
    private String merchant_code;
    private String erp_store_code;
    private Integer status;
    private Integer page_size;
    private String scroll_id;
    private Long start_time;
    private Long end_time;

    
    public String getMarket_user_id() {
        return market_user_id;
    }

    
    public void setMarket_user_id(String market_user_id) {
        this.market_user_id = market_user_id;
    }

    
    public String getMerchant_code() {
        return merchant_code;
    }

    
    public void setMerchant_code(String merchant_code) {
        this.merchant_code = merchant_code;
    }

    
    public String getErp_store_code() {
        return erp_store_code;
    }

    
    public void setErp_store_code(String erp_store_code) {
        this.erp_store_code = erp_store_code;
    }

    
    public Integer getStatus() {
        return status;
    }

    
    public void setStatus(Integer status) {
        this.status = status;
    }

    
    public Integer getPage_size() {
        return page_size;
    }

    
    public void setPage_size(Integer page_size) {
        this.page_size = page_size;
    }

    
    public String getScroll_id() {
        return scroll_id;
    }

    
    public void setScroll_id(String scroll_id) {
        this.scroll_id = scroll_id;
    }

    
    public Long getStart_time() {
        return start_time;
    }

    
    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    
    public Long getEnd_time() {
        return end_time;
    }

    
    public void setEnd_time(Long end_time) {
        this.end_time = end_time;
    }

}
