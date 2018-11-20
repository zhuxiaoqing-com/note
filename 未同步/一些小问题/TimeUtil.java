package com.sh.mh.web.commom;



import java.time.*;
import java.util.Date;
import java.util.function.Function;

/**
 * 时间类 工具类
 */
public class TimeUtil {
    /**
     *  增加相应的天数然后将时间返回
     * @param date
     * @return
     */
    public static Date plusDays(Date date, Integer day) {
        Date newDate = TimeUtil.getDateToFunction(date, (s)-> s.plusDays(day));

        return newDate;
    }

    /**
     * 根据 函数自己来进行 日期加减
     * @param date
     * @param function
     * @return
     */
    private static Date getDateToFunction(Date date, Function<LocalDate, LocalDate> function) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
        LocalDate localDate = localDateTime.toLocalDate();

        LocalDate localDate1 = function.apply(localDate);

        ZonedDateTime zonedDateTime = localDate1.atStartOfDay().atZone(zoneId);
        Instant instant1 = zonedDateTime.toInstant();

        return Date.from(instant1);

    }

    /**
     *  减少相应的天数然后将时间返回
     * @param date
     * @param day
     * @return
     */
    public static Date minusDays(Date date, Integer day) {
        Date newDate = getDateToFunction(date, (s) -> s.minusDays(day));
        return newDate;
    }


}
