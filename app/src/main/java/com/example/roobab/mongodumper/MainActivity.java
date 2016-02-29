package com.example.roobab.mongodumper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.wifi.WifiManager.RSSI_CHANGED_ACTION;
import static android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;

public class MainActivity extends AppCompatActivity {
    private static final int MSG_FETCH_WIFI_STRENGTH = 1;
    private static final long REFRESH_DURATION = 100;
    private TextView startRecordingView;
    private boolean shouldRecord = false;
    private TextView locationNameView;
    private SignalServer signalServer;
    private TextView stopRecordingView;
    private int numberOfTimes;
    private List<List<ScanResult>> apsArray;

    Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FETCH_WIFI_STRENGTH:
                    startScan();
                    break;
            }
        }
    };

    private void startScan() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiManager.startScan();
        setupWifiSignalReceivers();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signalServer = getSignalServer();
        numberOfTimes = 0;
        apsArray = new ArrayList<>();

        setUpViews();
        H.sendEmptyMessage(MSG_FETCH_WIFI_STRENGTH);
    }

    private void setUpViews() {
        locationNameView = (TextView) findViewById(R.id.locationName);
        startRecordingView = (TextView) findViewById(R.id.start);
        startRecordingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                H.sendEmptyMessage(MSG_FETCH_WIFI_STRENGTH);
                shouldRecord = true;
            }
        });
        stopRecordingView = (TextView) findViewById(R.id.stop);
        stopRecordingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldRecord = false;
                String locationName = locationNameView.getText().toString().trim();
                locationNameView.setText("");
                collectLocationSignals(locationName);
                unregisterReceivers();
            }
        });
    }

    private void collectLocationSignals(String locationName) {
        signalServer.collectLocationSignals(new TypedJsonString("{\"locationName\": \"" + locationName + "\"}"), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                System.out.println("Collect Success: " + response);
            }

            @Override
            public void failure(RetrofitError error) {
                System.out.println("Collect Error: " + error);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^in on receive of wifi signals");
            if(shouldRecord) {
                readSignalStrength();
            }
        }
    };

    private void setupWifiSignalReceivers() {
        registerReceiver(wifiReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(RSSI_CHANGED_ACTION));
    }

    private void unregisterReceivers() {
        unregisterReceiver(wifiReceiver);
    }

    private void readSignalStrength() {
        numberOfTimes++;
        String locationName = locationNameView.getText().toString().trim();
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        List<ScanResult> aps = wifiManager.getScanResults();
        List<ScanResult> filteredAps = new ArrayList<>();
        for(ScanResult ap : aps) {
//            if(ap.SSID.equals("twguest")) {
                filteredAps.add(ap);
//            }
        }

        if(shouldRecord) {
            if(numberOfTimes < 10) {
                apsArray.add(filteredAps);
                H.sendEmptyMessage(MSG_FETCH_WIFI_STRENGTH);
                return;
            }

            List<ScanResult> finalAps = getScanResultsWithMeanLevel(filteredAps);

            dumpIntoMongo(locationName, finalAps);
            H.sendEmptyMessageDelayed(MSG_FETCH_WIFI_STRENGTH, REFRESH_DURATION);
        } else {
            return;
        }
    }

    @Nullable
    private List<ScanResult> getScanResultsWithMeanLevel(List<ScanResult> filteredAps) {
        Map<String, Integer> macLevelMap = new HashMap<>();
        Map<String, Integer> macFreqMap = new HashMap<>();

        for(List<ScanResult> ap : apsArray) {
            for(ScanResult mac : ap) {
                Integer level = macLevelMap.get(mac.BSSID) != null ? macLevelMap.get(mac.BSSID) : 0;
                Integer freq = macFreqMap.get(mac.BSSID) != null ? macFreqMap.get(mac.BSSID) : 0;
                level += mac.level;
                freq += mac.frequency;
                macLevelMap.put(mac.BSSID, level);
                macFreqMap.put(mac.BSSID, freq);
            }
        }
        List<ScanResult> finalAps = apsArray.get(0);
        for(ScanResult finalAp : finalAps) {
            finalAp.level = macLevelMap.get(finalAp.BSSID)/10;
            finalAp.frequency = macFreqMap.get(finalAp.BSSID)/10;
        }
        numberOfTimes = 0;
        apsArray.clear();
        return finalAps;
    }

    private void dumpIntoMongo(String locationName, List<ScanResult> aps) {
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&in dumpintomongo");
        System.out.println("ScanREsult: " + aps);

        SpecificSignals specificSignals = new SpecificSignals(locationName, aps);

        signalServer.dumpSignals(specificSignals, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                System.out.println("Dump Success: " + response);
            }

            @Override
            public void failure(RetrofitError error) {
                System.out.println("Dump Error: " + error);
            }
        });
    }

    private SignalServer getSignalServer() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://10.132.124.180:9090")
                .build();
        return restAdapter.create(SignalServer.class);
    }
}
