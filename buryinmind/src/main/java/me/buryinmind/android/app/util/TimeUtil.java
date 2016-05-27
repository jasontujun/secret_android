package me.buryinmind.android.app.util;

import com.tj.xengine.android.utils.XLog;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by jasontujun on 2016/4/28.
 */
public abstract class TimeUtil {

    public static Calendar getCalendar(long timeInMillis) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(timeInMillis);
        calendar.setTimeZone(TimeZone.getDefault());// 从GMT时区转为本地时区
        return calendar;
    }

    /**
     * 根据出生时间，计算从出生到指定日期的年龄年龄。
     * @param birthTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @param curTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @return
     */
    public static int calculateAge(long birthTime, long curTime) {
        Calendar born = getCalendar(birthTime);
        Calendar now = getCalendar(curTime);
        if (born.after(now)) {
            XLog.d("TimeUtil", "Can't be born in the future");
            return 0;
        }
        int age = now.get(Calendar.YEAR) - born.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < born.get(Calendar.DAY_OF_YEAR)) {
            age -= 1;
        }
        return age;
    }

    /**
     * 根据出生时间，计算从出生到现在的年龄。
     * @param birthTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @return
     */
    public static int calculateAge(long birthTime) {
        return calculateAge(birthTime, Calendar.getInstance
                (TimeZone.getTimeZone("GMT")).getTimeInMillis());
    }
}
