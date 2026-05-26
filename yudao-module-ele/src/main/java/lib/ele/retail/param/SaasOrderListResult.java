package lib.ele.retail.param;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class SaasOrderListResult {

    private String errno;
    private String error;
    private MeEleRetailSaasOrderListResDto data;

    
    public String getErrno() {
        return errno;
    }

    
    public void setErrno(String errno) {
        this.errno = errno;
    }

    
    public String getError() {
        return error;
    }

    
    public void setError(String error) {
        this.error = error;
    }

    
    public MeEleRetailSaasOrderListResDto getData() {
        return data;
    }

    
    public void setData(MeEleRetailSaasOrderListResDto data) {
        this.data = data;
    }

}
