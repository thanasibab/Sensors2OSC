package org.sensors2.osc.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;

import org.sensors2.osc.dispatch.OscConfiguration;
import org.sensors2.osc.dispatch.OscDispatcher;

import org.sensors2.osc.R;
import org.sensors2.osc.sensors.Settings;

import java.util.Date;

public class RecordingActivity extends Activity implements SensorEventListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private OscDispatcher dispatcher;
    private OSCPortIn receiver;
    private Settings settings;
    private boolean connectedToDevice = false;
    LocationManager locationManager;
    LocationListener locationListener;
    private SensorManager sensorManager;
    private float longitude, latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        this.dispatcher = new OscDispatcher();
        this.settings = this.loadSettings();
        this.initOscPortIn();
        changeRecButtonColor(0xFF00FF00);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ASK_PERMISSIONS);
            }
            else {
                return;
            }
        }
        try {
            latitude = (float)locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
            longitude = (float)locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }
        catch (Exception e){
            System.out.println("Could not get last known location.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_guide: {
                Intent intent = new Intent(this, GuideActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_sensors: {
                Intent intent = new Intent(this, SensorsActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public Settings getSettings() {
        return this.settings;
    }

    public void startStopRecording(View view) {
        if(connectedToDevice){
            dispatcher.trySend("toggleRecording");
        }
    }

    public void connectToDevice(View view) {
        dispatcher.trySend("connectToDevice");
    }

    private Settings loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Settings settings = new Settings(preferences);
        OscConfiguration oscConfiguration = OscConfiguration.getInstance();
        oscConfiguration.setHost(settings.getHost());
        oscConfiguration.setPort(settings.getPort());
        return settings;
    }

    public void changeRecButtonColor(final int color) {
        RecordingActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button rec_btn = (Button) findViewById(R.id.rec_button);
                rec_btn.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        });
    }

    public void changeRecViewText(final String text) {
        RecordingActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView rec_text = (TextView) findViewById(R.id.rec_text);
                rec_text.setText(text);
            }
        });
    }

    public void initOscPortIn() {
        if(this.receiver == null) {
            try {
                receiver = new OSCPortIn(7001);
                receiver.addListener("/recStarted", new OSCListener() {

                    @Override
                    public void acceptMessage(Date date, OSCMessage oscMessage) {
                        changeRecViewText("Recording...");
                        changeRecButtonColor(0xFFFF0000);
                    }
                });
                receiver.addListener("/recStopped", new OSCListener() {
                    @Override
                    public void acceptMessage(Date date, OSCMessage oscMessage) {
                        changeRecViewText("Connected. Press to start...");
                        changeRecButtonColor(0xFF0000FF);
                    }
                });
                receiver.addListener("/sensorInitialized", new OSCListener() {
                    @Override
                    public void acceptMessage(Date date, OSCMessage oscMessage) {
                        changeRecViewText("Connected. Press to start...");
                        dispatcher.trySend("latitude", latitude);
                        dispatcher.trySend("longitude", longitude);
                        changeRecButtonColor(0xFF0000FF);
                        connectedToDevice = true;
                    }
                });
                receiver.addListener("/connectDevice", new OSCListener() {
                    @Override
                    public void acceptMessage(Date date, OSCMessage oscMessage) {
                        changeRecViewText("Please connect camera...");
                        changeRecButtonColor(0xFF0000FF);
                    }
                });
                receiver.addListener("/disconnected", new OSCListener() {
                    @Override
                    public void acceptMessage(Date date, OSCMessage oscMessage) {
                        changeRecViewText("Waiting for connection...");
                        changeRecButtonColor(0xFF00FF00);
                    }
                });
                receiver.startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*---------- Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            dispatcher.trySend("longitude", (float)loc.getLongitude());
            dispatcher.trySend("latitude", (float)loc.getLatitude());
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            dispatcher.trySend("accelerometer", event.values);
        }
        else if (event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
            dispatcher.trySend("linearacceleration", event.values);
        }
    }
}
