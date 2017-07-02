package Util;

import java.io.PrintWriter;
import java.io.StringWriter;

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
}
