
package com.alibaba.ocean.rawsdk.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public final class DateUtil {
        public static final String DEFAULT_DATE_FORMAT_STR = "yyyyMMddHHmmssSSSZ";

            public static final String SIMPLE_DATE_FORMAT_STR = "yyyy-MM-dd HH:mm:ss.SSS";

    private static SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat(DEFAULT_DATE_FORMAT_STR);

    private DateUtil() {
    }

    public static String format(Date d) {
        return format(d, null);
    }

    public static String format(Date d, String pattern) {
        return format(d, pattern, null);
    }

    public static String format(Date d, String pattern, TimeZone timeZone) {
        if (d == null) {
            return null;
        }
        final SimpleDateFormat format;
        if (pattern != null) {
            format = new SimpleDateFormat(pattern);
        } else {
            format = (SimpleDateFormat) DEFAULT_FORMAT.clone();
        }
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        return format.format(d);
    }

    public static Date parse(String str) throws ParseException {
        return parse(str, null);
    }

    public static Date parse(String source, String pattern) throws ParseException {
        return parse(source, pattern, null);
    }

    public static Date parse(String source, String pattern, TimeZone timeZone) throws ParseException {
        if (source == null) {
            return null;
        }
        final SimpleDateFormat format;
        if (pattern != null) {
            format = new SimpleDateFormat(pattern);
        } else {
            format = (SimpleDateFormat) DEFAULT_FORMAT.clone();
        }
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        return format.parse(source);
    }

    ;
}
