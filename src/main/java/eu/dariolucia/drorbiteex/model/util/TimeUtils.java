package eu.dariolucia.drorbiteex.model.util;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    public static AbsoluteDate toAbsoluteDate(Date time) {
        return new AbsoluteDate(time, TimeScalesFactory.getUTC());
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public static String formatDate(Date d) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(d);
    }

}
