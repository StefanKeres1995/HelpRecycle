package pt.ipleiria.helprecycle.gps;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class GPSPoint {

    public float latitude;
    public float longitude;

    public GPSPoint() {}

    public GPSPoint(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
