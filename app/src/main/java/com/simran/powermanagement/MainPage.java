package com.simran.powermanagement;


import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.util.Calendar;

public class MainPage extends AppCompatActivity {

    ImageView devicesBtn, shareBtn, thermostatBtn, cameraBtn;
    FragmentTransaction fragmentTransaction;

    View.OnClickListener deviceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            loadFragment(new DeviceMain_F());
        }
    };

    View.OnClickListener shareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent mInt = new Intent(MainPage.this, MqttShared.class);
            startService(mInt);

            loadFragment(new ShareMain_F());

        }
    };

    View.OnClickListener thermostatClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener cameraClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the toolbar_main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menuMic:
                Intent deviceIntent = new Intent(MainPage.this, VoiceActivity.class);
                startActivity(deviceIntent);
                return true;
            case R.id.menuAccount:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        loadFragment(new DeviceMain_F());

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        // setting Toolbar as Action Bar for the App
        setSupportActionBar(mToolbar);

        //Always run it when main Page executes
        Mqtt.IS_SERVICE_RUNNING = false;

        devicesBtn = findViewById(R.id.devicesBtn);
        devicesBtn.setOnClickListener(deviceClickListener);
        // devicesBtn.setOnLongClickListener(deviceClickListenerLong);

        shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(shareClickListener);

        thermostatBtn = findViewById(R.id.thermostatBtn);
        thermostatBtn.setOnClickListener(thermostatClickListener);

        cameraBtn = findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener(cameraClickListener);

    }


    private void loadFragment(Fragment fragment) {

        // Begin the transaction
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_left_enter, R.anim.fragment_slide_left_exit, R.anim.fragment_slide_right_enter, R.anim.fragment_slide_right_exit);

        // Replace the contents of the container with the new fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        // or ft.add(R.id.your_placeholder, new FooFragment());
        // Complete the changes added above
        fragmentTransaction.commit();
    }


    public void dialog(String title, String message) {
        android.app.AlertDialog.Builder builder;
        builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })


                /*
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                */
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (true) {
            //
        } else {
            super.onBackPressed();
        }
    }

}

