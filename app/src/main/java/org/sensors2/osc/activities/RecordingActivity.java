package org.sensors2.osc.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class RecordingActivity extends Activity {

    private OscDispatcher dispatcher;
    private OSCPortIn receiver;
    private Settings settings;
    private boolean connectedToDevice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        this.dispatcher = new OscDispatcher();
        this.settings = this.loadSettings();

        this.initOscPortIn();

        changeRecButtonColor(0xFF00FF00);
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
                        changeRecButtonColor(0xFF0000FF);
                        connectedToDevice = true;
                    }
                });
                receiver.startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}