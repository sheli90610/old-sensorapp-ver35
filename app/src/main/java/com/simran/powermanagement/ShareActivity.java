package com.simran.powermanagement;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ShareActivity extends AppCompatActivity {

    public static String currentMACForSharedOnOff;
    ImageView powerBtnTemp;

    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
    private String status;
    View.OnClickListener publishClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (status.equals("on")) {
                status = "off";
                turnOFF();
                powerBtnTemp.setImageResource(R.drawable.power_off);
            } else if (status.equals("off")) {
                status = "on";
                turnON();
                powerBtnTemp.setImageResource(R.drawable.power_on);
            }
        }
    };
    private ImageView electricityPlugImg, schImg, temperatureMercuryImg;
    private ProgressBar simpleProgressBar;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            status = intent.getStringExtra("Status");

            if (status.equals("on")) {
                powerBtnTemp.setImageResource(R.drawable.power_on);
                simpleProgressBar.setVisibility(View.INVISIBLE);
                powerBtnTemp.setEnabled(true);
                powerBtnTemp.setVisibility(View.VISIBLE);
            } else if (status.equals("off")) {
                powerBtnTemp.setImageResource(R.drawable.power_off);
                simpleProgressBar.setVisibility(View.INVISIBLE);
                powerBtnTemp.setEnabled(true);
                powerBtnTemp.setVisibility(View.VISIBLE);
            }
        }
    };
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_devices);                             //Opens up the main view

        powerBtnTemp = findViewById(R.id.powerBtnTemp);
        powerBtnTemp.setOnClickListener(publishClickListener);
        powerBtnTemp.setImageResource(R.drawable.power_disabled);
        powerBtnTemp.setVisibility(View.INVISIBLE);

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);
        simpleProgressBar.setVisibility(View.VISIBLE);

        LocalBroadcastManager.getInstance(ShareActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("ShareActivity"));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkStatus();
            }
        }, 100);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void checkStatus() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MqttShared.MSG_STATUS_OF_DEVICE_SHARED, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void turnON() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MqttShared.MSG_ON_SHARED, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void turnOFF() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MqttShared.MSG_OFF_SHARED, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, MqttShared.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


}
