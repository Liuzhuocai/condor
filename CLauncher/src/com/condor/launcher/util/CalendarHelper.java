package com.condor.launcher.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Perry on 19-2-18
 */
public class CalendarHelper {
    public CalendarHelper() {
    }

    private static Calendar getCalendar() {
        return Calendar.getInstance();
    }

    public static int getYear() {
        return getCalendar().get(Calendar.YEAR);
    }

    public static int getMonth() {
        return getCalendar().get(Calendar.MONTH) + 1;
    }

    public static int getDay() {
        return getCalendar().get(Calendar.DAY_OF_MONTH);
    }

    public static int getWeekday() {
        int foreignWeek = getCalendar().get(Calendar.DAY_OF_WEEK);
        int chinaWeek = foreignWeek - 1;
        if(chinaWeek == 0) {
            chinaWeek = 7;
        }

        return chinaWeek;
    }

    public static int getHour() {
        return getCalendar().get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute() {
        return getCalendar().get(Calendar.MINUTE);
    }

    public static int getSecond() {
        return getCalendar().get(Calendar.SECOND);
    }

    public static int getAMPM() {
        return getCalendar().get(Calendar.AM_PM);
    }

    public static String getFormatDate(String format) {
        return new SimpleDateFormat(format, Locale.getDefault()).format(new Date());
    }
}
