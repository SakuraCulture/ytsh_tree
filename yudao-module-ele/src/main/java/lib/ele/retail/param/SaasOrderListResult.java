package lib.ele.retail.param;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class SaasOrderListResult {

    private String errno;
    private String error;
    private MeEleRetailSaasOrderListResDto data;

    /**
     * @return 返回错误�?
     */
    public String getErrno() {
        return errno;
    }

    /**
     * 设置返回错误�?    *
     * <p>
     * 此参数必�?
     */
    public void setErrno(String errno) {
        this.errno = errno;
    }

    /**
     * @return 返回错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 设置返回错误信息     *
     * <p>
     * 此参数必�?
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * @return 响应结果
     */
    public MeEleRetailSaasOrderListResDto getData() {
        return data;
    }

    /**
     * 设置响应结果     *
     * <p>
     * 此参数必�?
     */
    public void setData(MeEleRetailSaasOrderListResDto data) {
        this.data = data;
    }

}
