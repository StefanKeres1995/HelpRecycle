package pt.ipleiria.helprecycle.Maps;

import java.util.ArrayList;

public class GPSCoordinates {

    private String name;
    private Double latitude;
    private Double longitude;

    public GPSCoordinates() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public GPSCoordinates(String name, Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public String getName() {
        return name;
    }

    public Double getLongitude() {
        return longitude;
    }
}
