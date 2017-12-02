package pe.joyyir.Heungbubak.Common.Util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CmnUtil {
    public static long msTime() {
        return System.currentTimeMillis();
    }

    public static long nsTime() {
        return System.nanoTime();
    }

    public static String getStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    // ex) 2017.07.20 19:37:42
    public static String timeToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy.MM.dd HH:mm:ss]");
        return dateFormat.format(date);
    }

    public static boolean isNotEmpty(String string) {
        return (string != null && !"".equals(string));
    }

    public static boolean isEmpty(String string) {
        return !isNotEmpty(string);
    }

    public static boolean isNotEmpty(List<?> list) {
        return (list != null && list.size() > 0);
    }

    public static boolean isEmpty(List<?> list) {
        return !isNotEmpty(list);
    }
}
