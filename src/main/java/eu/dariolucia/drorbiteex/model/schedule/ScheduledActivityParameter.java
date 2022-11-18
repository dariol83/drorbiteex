package eu.dariolucia.drorbiteex.model.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ScheduledActivityParameter {

    public static ScheduledActivityParameter timeParameterB(String name, Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new ScheduledActivityParameter("timeParameterB", name, sdf.format(value));
    }

    public static ScheduledActivityParameter timeParameterA(String name, Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new ScheduledActivityParameter("timeParameterA", name, sdf.format(value));
    }

    public static ScheduledActivityParameter booleanParameter(String name, boolean value) {
        return new ScheduledActivityParameter("booleanParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter intParameter(String name, int value) {
        return new ScheduledActivityParameter("intParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter unsignedIntParameter(String name, long value) {
        return new ScheduledActivityParameter("unsignedIntParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter floatParameter(String name, float value) {
        return new ScheduledActivityParameter("floatParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter doubleParameter(String name, double value) {
        return new ScheduledActivityParameter("doubleParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter stringParameter(String name, String value) {
        return new ScheduledActivityParameter("stringParameter", name, value);
    }

    private final String tagName;
    private final String parameterName;
    private final String parameterValue;

    private ScheduledActivityParameter(String tagName, String parameterName, String parameterValue) {
        this.tagName = tagName;
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getTagName() {
        return tagName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }
}
