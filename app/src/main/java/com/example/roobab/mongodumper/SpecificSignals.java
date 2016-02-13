package com.example.roobab.mongodumper;

import android.net.wifi.ScanResult;

import java.util.List;

public class SpecificSignals {
    private final String locationName;
    private final List<ScanResult> aps;

    public SpecificSignals(String locationName, List<ScanResult> aps) {
        this.locationName = locationName;
        this.aps = aps;
    }
}
