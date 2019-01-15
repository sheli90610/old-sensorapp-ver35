package com.simran.powermanagement;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventService extends IntentService {

    static final String LOG_TAG = "EventService";
    public static List<String> deviceList = new ArrayList<>();

    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
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

    public EventService() {
        super("EventService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //sendNotification("Wake Up! Wake Up!");
    }

    public void devices(String str) {
        deviceList.add(str);
    }

    private void turnON() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, Mqtt.MSG_ON, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void turnOFF() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, Mqtt.MSG_OFF, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bindService(new Intent(this, Mqtt.class), mConnection, Context.BIND_AUTO_CREATE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                execute();
            }
        }, 100);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void execute() {
        JSONObject jsON = null, jsOFF = null;

        try {

            jsON = new JSONObject();
            jsOFF = new JSONObject();
            jsON.put("ONOFF", "(on)");
            jsOFF.put("ONOFF", "(off)");

            //Getting mac address and state for mqtt
            String mac = FileHelper.readLine("schs.txt", 0);
            String state = FileHelper.readLine("schs.txt", 2);

            Mqtt.macAddress = mac;

            if ("Turn On".equals(state)) {
                turnON();
            }
            if ("Turn Off".equals(state)) {
                turnOFF();
            }
            Log.i(LOG_TAG, "Schedule Executed for mac " + Mqtt.macAddress);


        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Keep a record before deleting event from file to check if next next schedule is less than 5 min
                    String finalTimeRecord = FileHelper.readLine("schs.txt", 3);
                    String[] arrayRecord = finalTimeRecord.split("/");
                    String yearRecord = arrayRecord[0];
                    String monthRecord = arrayRecord[1];
                    String dateRecord = arrayRecord[2];
                    String hourRecord = arrayRecord[3];
                    String minuteRecord = arrayRecord[4];

                    //Read the mac address from universal schedule file
                    String mac = FileHelper.readLine("schs.txt", 0);
                    //Read the unique name from universal schedule file
                    String uName = FileHelper.readLine("schs.txt", 1);
                    //Return Line number containing unique Name from single schedule file
                    //Done use mac address as search citerion because universal file might have multiple same mac addresses
                    int lineNumber = FileHelper.returnLineNumber(uName, "sch" + mac + ".txt") - 2;

                    //Delete the latest schedule which has been executed
                    for (int i = 0; i < 4; i++) {
                        FileHelper.removeLine("sch" + mac + ".txt", lineNumber);
                        FileHelper.removeLine("schs.txt", 0);
                    }

                    //This is the latest information in file
                    String finalTime = FileHelper.readLine("schs.txt", 3);

                    if(!finalTime.isEmpty() && !finalTime.equals("")) {
                        String[] array = finalTime.split("/");
                        String year = array[0];
                        String month = array[1];
                        String date = array[2];
                        String hour = array[3];
                        String minute = array[4];

                        //Testing Next schedule if its less than 5 mins it will be executed immediately
                        //If its true it means hour is same
                        if (year.equals(yearRecord)) {
                            if (month.equals(monthRecord)) {
                                if (date.equals(dateRecord)) {
                                    if (hour.equals(hourRecord)) {

                                        boolean sameMinutes = minute.equals(minuteRecord);
                                        int lessMinutes = Integer.parseInt(minute) - Integer.parseInt(minuteRecord);

                                        //If its true it means difference last and current schedule is less than 5 mins or its same
                                        if (sameMinutes || lessMinutes <= 5) {
                                            execute();
                                        }
                                    }
                                }
                            }
                        }
                    }


                    //Call this to schedule NextEvent
                    Intent service = new Intent(getApplicationContext(), EventMonitor.class);
                    getApplicationContext().startService(service);

                    //SelfDestroy this service which executes scheduled events
                    Log.e("SelfDestruction", "EventService will be destroyed");
                    service = new Intent(getApplicationContext(), EventService.class);
                    getApplicationContext().stopService(service);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}