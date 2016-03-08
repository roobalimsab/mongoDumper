package com.example.roobab.mongodumper;

import android.net.wifi.ScanResult;

import java.util.List;

public class LocationSpecificSignals {
    private final String locationName;
    private final List<ScanResult> aps;

    public LocationSpecificSignals(String locationName, List<ScanResult> aps) {
        this.locationName = locationName;
        this.aps = aps;
    }
}
