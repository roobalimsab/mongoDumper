package com.example.roobab.mongodumper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.wifi.WifiManager.RSSI_CHANGED_ACTION;
import static android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int MSG_FETCH_WIFI_STRENGTH = 1;
    private static final long REFRESH_DURATION = 100;
    private TextView startRecordingView;
    private boolean shouldRecord = false;
    private TextView locationNameView;
    private SignalServer signalServer;
    private TextView stopRecordingView;

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
    private SensorManager sensorManager;
    private Sensor magneticFieldSensor;

    private void startScan() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiManager.startScan();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signalServer = getSignalServer();
        setupSensorReceivers();

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
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
        setupWifiSignalReceivers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        unregisterReceivers();
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

    private void setupSensorReceivers() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    private void setupWifiSignalReceivers() {
        registerReceiver(wifiReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(RSSI_CHANGED_ACTION));
    }

    private void unregisterReceivers() {
        unregisterReceiver(wifiReceiver);
    }

    private void readSignalStrength() {
        String locationName = locationNameView.getText().toString().trim();
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        List<ScanResult> aps = wifiManager.getScanResults();
        dumpIntoMongo(locationName, aps);
        H.sendEmptyMessageDelayed(MSG_FETCH_WIFI_STRENGTH, REFRESH_DURATION);
    }

    private void dumpIntoMongo(String locationName, List<ScanResult> aps) {
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&in dumpintomongo");
        System.out.println("ScanREsult: " + aps);

        SpecificSignals specificSignals = new SpecificSignals(locationName, aps);
        TypedJsonString signalJson = new TypedJsonString(new Gson().toJson(specificSignals));
        System.out.println("signaljson: " + signalJson);

        signalServer.dumpSignals(signalJson, new Callback<Response>() {
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
                .setEndpoint("http://192.168.0.34:9090")
                .build();
        return restAdapter.create(SignalServer.class);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@event: " + event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
