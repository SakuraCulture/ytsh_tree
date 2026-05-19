package cn.iocoder.yudao.module.ele.util;

import java.math.BigDecimal;

public class MoneyUtils {
    
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    
    public static BigDecimal fenToYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(HUNDRED, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public static Long parseStringToLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
