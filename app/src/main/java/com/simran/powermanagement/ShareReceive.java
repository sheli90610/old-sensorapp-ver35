package com.simran.powermanagement;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ShareReceive extends AppCompatActivity {

    protected static String word;
    TextView uniqueWord;
    ImageView startShareBtn;
    Messenger mService = null;
    boolean mBound;

    View.OnClickListener startShareBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            word = uniqueWord.getText().toString();

            if (word.equals("")) {
                Toast.makeText(ShareReceive.this, "Please enter a name", Toast.LENGTH_SHORT).show();
            } else {

                SharedPreferences.Editor editor = getSharedPreferences("myShare", MODE_PRIVATE).edit();
                editor.putString("topic", word);
                editor.apply();
                shareReceive();
            }

        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent

            String name = intent.getStringExtra("Status");
            Toast.makeText(getApplicationContext(), name + " Device Received", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receive);

        uniqueWord = findViewById(R.id.uniqueWord);
        startShareBtn = findViewById(R.id.startRecvBtn);

        startShareBtn.setOnClickListener(startShareBtnClickListener);

        LocalBroadcastManager.getInstance(ShareReceive.this).registerReceiver(mMessageReceiver, new IntentFilter("ShareReceive"));

    }

    public void shareReceive() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MqttShared.MSG_SHARE_RECEIVE, 0, 0);
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
