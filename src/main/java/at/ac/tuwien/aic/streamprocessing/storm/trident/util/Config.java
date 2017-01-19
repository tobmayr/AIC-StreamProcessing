package at.ac.tuwien.aic.streamprocessing.storm.trident.util;

public class Config {
    private Config() {

    }

    // Allowed Area configuration
    public static final Double ALLOWED_AREA_CENTER_LAT = 39.916320;
    public static final Double ALLOWED_AREA_CENTER_LONG = 116.397155;
    public static final Double WARNING_DISTANCE = 10D;

    // Speeding configuration
    public static final Double SPEED_LIMIT = 50D;

    // Dashboard sub-URI configuration
    public static final String PROPAGATE_LOCATION_URI = "/add";
    public static final String PROPAGATE_INFORMATION_URI = "/stats";
    public static final String NOTIFY_AREA_VIOLATION_URI = "/violation";
    public static final String NOTIFY_SPEEDING_INCIDENT_URI = "/incident";
    public static final String NOTIFY_TAXI_STOPPED_URI = "/stop";

}