package com.example.roobab.mongodumper;

import android.net.wifi.ScanResult;

import java.util.List;

public class LocationSpecificSignal {
    private final String locationName;
    private final String BSSID;
    private final int level;

    public LocationSpecificSignal(String locationName, String BSSID, int level) {
        this.locationName = locationName;
        this.BSSID = BSSID;
        this.level = level;
    }
}
