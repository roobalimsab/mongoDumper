package com.example.roobab.mongodumper;

import android.net.wifi.ScanResult;

public class SpecificSignals {
    private final String locationName;
    private final ScanResult ap;

    public SpecificSignals(String locationName, ScanResult ap) {
        this.locationName = locationName;
        this.ap = ap;
    }
}
