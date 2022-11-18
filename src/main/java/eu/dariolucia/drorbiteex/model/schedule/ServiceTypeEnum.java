package eu.dariolucia.drorbiteex.model.schedule;

public enum ServiceTypeEnum {
    APA_AZ_EL("APA-AZ/EL"),
    APA_X_Y("APA-X/Y"),
    DELTADOR("DELTADOR"),
    DOPPLER("DOPPLER"),
    OFFLINE_TMRECORDING("OFFLINE-TMRECORDING"),
    OFFLINE_TMPROVISION("OFFLINE-TMPROVISION"),
    RF_ONLY("RF-ONLY"),
    RANGING("RANGING"),
    RESERVED("RESERVED"),
    TBD("TBD"),
    TELECOMMAND("TELECOMMAND"),
    TELEMETRY("TELEMETRY"),
    TEST("TEST"),
    UNUSED("UNUSED"),
    VLBI("VLBI");

    private String type;

    ServiceTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getType();
    }
}
