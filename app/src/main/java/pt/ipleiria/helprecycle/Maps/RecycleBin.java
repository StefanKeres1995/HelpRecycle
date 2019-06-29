package pt.ipleiria.helprecycle.Maps;

import com.google.android.gms.maps.model.LatLng;

public class RecycleBin {

    private String title;
    private String snippet;
    private LatLng location;

    public RecycleBin(String title, LatLng location) {
        this.title = title;
        this.snippet = "Interact with " + title + "?";
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public LatLng getLocation() {
        return location;
    }
}
