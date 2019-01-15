package com.simran.powermanagement;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ShareSend extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    protected static String word;
    private static String[] paths = null;
    TextView uniqueWord;
    ImageView startSendBtn;
    Messenger mService = null;
    boolean mBound;
    private Spinner spinner;

    View.OnClickListener startSendBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            word = uniqueWord.getText().toString();

            if (word.equals("")) {
                Toast.makeText(ShareSend.this, "Please enter a name", Toast.LENGTH_SHORT).show();
            } else {

                String name = spinner.getSelectedItem().toString();

                int lineNum = FileHelper.returnLineNumber(name, FileHelper.mainDevices);
                String mac = FileHelper.readLine(FileHelper.mainDevices, lineNum-2);
                String icon = FileHelper.readLine(FileHelper.mainDevices, lineNum);

                SharedPreferences.Editor editor = getSharedPreferences("myShare", MODE_PRIVATE).edit();
                editor.putString("topic", word);
                editor.putString("name", name);
                editor.putString("mac", mac);
                editor.putString("icon", icon);
                editor.apply();
                shareSend();
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

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_send);

        uniqueWord = findViewById(R.id.uniqueWord);
        startSendBtn = findViewById(R.id.startSendBtn);

        startSendBtn.setOnClickListener(startSendBtnClickListener);

        int numberOfDevices = (FileHelper.countItems(FileHelper.mainDevices)) / 3;

        paths = new String[numberOfDevices];
        FileHelper.readNameForShare();

        spinner = findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ShareSend.this, R.layout.spinner, paths);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    public void setDeviceList(int i, String name) {
        paths[i] = name;
    }

    public void shareSend() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MqttShared.MSG_SHARE_SEND, 0, 0);
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