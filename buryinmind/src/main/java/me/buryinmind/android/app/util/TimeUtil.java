package me.buryinmind.android.app.util;

import com.tj.xengine.android.utils.XLog;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by jasontujun on 2016/4/28.
 */
public abstract class TimeUtil {

    /**
     * 获取更改时区后的Unix时间戳
     * @param time 旧时区下的Unix时间戳
     * @param oldZone 旧时区对象
     * @param newZone 新时区对象
     * @return 转换后的Unix时间戳
     */
    public static long changeTimeZone(long time, TimeZone oldZone, TimeZone newZone) {
        int timeOffset = oldZone.getRawOffset() - newZone.getRawOffset();
        return time - timeOffset;
    }

    public static long changeTimeZoneToUTC(long time) {
        return changeTimeZone(time, TimeZone.getDefault(), TimeZone.getTimeZone("GMT"));
    }

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
