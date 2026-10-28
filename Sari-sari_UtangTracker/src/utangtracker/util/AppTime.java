package utangtracker.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class AppTime {
    public static final TimeZone MANILA = TimeZone.getTimeZone("Asia/Manila");
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private AppTime() {
    }

    public static String now() {
        return format(new Date());
    }

    public static String format(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(PATTERN);
        format.setTimeZone(MANILA);
        return format.format(date);
    }

    public static String minutesFromNow(int minutes) {
        return format(new Date(System.currentTimeMillis() + minutes * 60L * 1000L));
    }

    public static String todayPrefix() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(MANILA);
        return format.format(new Date());
    }

    public static String daysAgo(int days, String time) {
        Date date = new Date(System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(MANILA);
        return format.format(date) + " " + time;
    }

    public static String peso(double amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return "PHP " + format.format(amount);
    }
}
