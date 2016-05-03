package me.buryinmind.android.app.util;

import com.tj.xengine.android.utils.XLog;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by jasontujun on 2016/4/28.
 */
public class TimeUtil {

    /**
     * 根据出生时间，计算年龄。
     * @param birthTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @param curTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @return
     */
    public static int calculateAge(long birthTime, long curTime) {
        Calendar born = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("GMT"));
        born.setTimeZone(TimeZone.getTimeZone("GMT"));
        now.setTimeInMillis(curTime);
        born.setTimeInMillis(birthTime);
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
     * 根据出生时间，计算年龄。
     * @param birthTime 从1970年1月1日起,以GMT时区计算的毫秒数
     * @return
     */
    public static int calculateAge(long birthTime) {
        return calculateAge(birthTime, Calendar.getInstance
                (TimeZone.getTimeZone("GMT")).getTimeInMillis());
    }
}
