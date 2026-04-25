package lib.ele.retail.param;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class MeEleRetailSaasOrderListResDto {

    private String scroll_id;
    private MeEleRetailSaasOrderListDetailResDto[] order_list;
    private Long total;

    /**
     * @return 游标值，如果需要翻页查询，下一次需要传入此游标
     */
    public String getScroll_id() {
        return scroll_id;
    }

    /**
     * 设置游标值，如果需要翻页查询，下一次需要传入此游标     *
     * 参数示例�?pre>c5639406aee57502</pre>
     * 此参数必�?
     */
    public void setScroll_id(String scroll_id) {
        this.scroll_id = scroll_id;
    }

    /**
     * @return 订单列表
     */
    public MeEleRetailSaasOrderListDetailResDto[] getOrder_list() {
        return order_list;
    }

    /**
     * 设置订单列表     *
     * 参数示例�?pre>[]</pre>
     * 此参数必�?
     */
    public void setOrder_list(MeEleRetailSaasOrderListDetailResDto[] order_list) {
        this.order_list = order_list;
    }

    /**
     * @return 订单总数
     */
    public Long getTotal() {
        return total;
    }

    /**
     * 设置订单总数     *
     * 参数示例�?pre>100</pre>
     * 此参数必�?
     */
    public void setTotal(Long total) {
        this.total = total;
    }

}
