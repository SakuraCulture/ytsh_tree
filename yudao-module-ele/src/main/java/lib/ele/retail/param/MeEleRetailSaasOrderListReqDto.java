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

    /**
     * @return 服务市场用户id（等于饿了么店铺id，服务市场场景必填）
     */
    public String getMarket_user_id() {
        return market_user_id;
    }

    /**
     * 设置服务市场用户id（等于饿了么店铺id，服务市场场景必填）     *
     * 参数示例�?pre>1135859202</pre>
     * 此参数必�?
     */
    public void setMarket_user_id(String market_user_id) {
        this.market_user_id = market_user_id;
    }

    /**
     * @return saas商家编码（erp接入必填�?
     */
    public String getMerchant_code() {
        return merchant_code;
    }

    /**
     * 设置saas商家编码（erp接入必填�?    *
     * 参数示例�?pre>m123</pre>
     * 此参数必�?
     */
    public void setMerchant_code(String merchant_code) {
        this.merchant_code = merchant_code;
    }

    /**
     * @return 外部门店编码（erp接入场景必填�?
     */
    public String getErp_store_code() {
        return erp_store_code;
    }

    /**
     * 设置外部门店编码（erp接入场景必填�?    *
     * 参数示例�?pre>store123</pre>
     * 此参数必�?
     */
    public void setErp_store_code(String erp_store_code) {
        this.erp_store_code = erp_store_code;
    }

    /**
     * @return 订单状态，1（已支付）�?（已接单）�?（已拣货）�?（已打包）�?（已发货）�?（交易成功）�?1（交易关闭）
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置订单状态，1（已支付）�?（已接单）�?（已拣货）�?（已打包）�?（已发货）�?（交易成功）�?1（交易关闭）     *
     * 参数示例�?pre>1</pre>
     * 此参数必�?
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * @return 每页订单数量，范围：[1-100]，不传默认返�?20�?
     */
    public Integer getPage_size() {
        return page_size;
    }

    /**
     * 设置每页订单数量，范围：[1-100]，不传默认返�?20�?    *
     * 参数示例�?pre>20</pre>
     * 此参数必�?
     */
    public void setPage_size(Integer page_size) {
        this.page_size = page_size;
    }

    /**
     * @return 游标，第一次查询时无需传入游标，如需翻页，后续每次查询都需要将上一次接口返回的游标代入
     */
    public String getScroll_id() {
        return scroll_id;
    }

    /**
     * 设置游标，第一次查询时无需传入游标，如需翻页，后续每次查询都需要将上一次接口返回的游标代入     *
     * 参数示例�?pre>c5639406aee57502</pre>
     * 此参数必�?
     */
    public void setScroll_id(String scroll_id) {
        this.scroll_id = scroll_id;
    }

    /**
     * @return 起始时间，不传默认只查询当天订单，仅可查询近3个月内的订单，格式：unix时间戳（秒级�?
     */
    public Long getStart_time() {
        return start_time;
    }

    /**
     * 设置起始时间，不传默认只查询当天订单，仅可查询近3个月内的订单，格式：unix时间戳（秒级�?    *
     * 参数示例�?pre>1619020800</pre>
     * 此参数必�?
     */
    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    /**
     * @return 结束时间，不传默认只查询当天订单，格式：unix时间戳（秒级�?
     */
    public Long getEnd_time() {
        return end_time;
    }

    /**
     * 设置结束时间，不传默认只查询当天订单，格式：unix时间戳（秒级�?    *
     * 参数示例�?pre>1618848000</pre>
     * 此参数必�?
     */
    public void setEnd_time(Long end_time) {
        this.end_time = end_time;
    }

}
