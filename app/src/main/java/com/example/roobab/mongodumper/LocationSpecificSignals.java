package com.example.roobab.mongodumper;

public class LocationSpecificSignals {
    private final String locationName;
    private final String BSSID;
    private final int level;

    public LocationSpecificSignals(String locationName, String BSSID, int level) {
        this.locationName = locationName;
        this.BSSID = BSSID;
        this.level = level;
    }


}
