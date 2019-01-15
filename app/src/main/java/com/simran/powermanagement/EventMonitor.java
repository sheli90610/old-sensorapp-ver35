package com.simran.powermanagement;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventMonitor extends Service {

    public static List<String> deviceList = new ArrayList<>();
    AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static boolean IS_SERVICE_RUNNING = false;

    public void devices(String str) {
        deviceList.add(str);
    }

    //Execution of this service indicates following:
    //The device was restarted
    //A new event was created by user
    //Previous one was completed successfully
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        FileHelper.readMacForEventMonitor();

        new Thread(new Runnable() {
            @Override
            public void run() {
                scheduleAlarms();
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void scheduleAlarms() {

        //If no alarms are found then stop all related services
        if (FileHelper.readLine("schs.txt", 0).equals("")) {

            //SelfDestroy if there is nothing inside the file
			Log.e("SelfDestruction", "EventMonitor will be destroyed");

            Intent service = new Intent(getApplicationContext(), EventMonitor.class);
            getApplicationContext().stopService(service);
            return;
        }

        String mac = FileHelper.readLine("schs.txt", 0);

        String finalTime = FileHelper.readLine("schs.txt", 3);

        String[] array = finalTime.split("/");
        int year = Integer.parseInt(array[0]);
        int month = Integer.parseInt(array[1]);
        int date = Integer.parseInt(array[2]);
        int hour = Integer.parseInt(array[3]);
        int minute = Integer.parseInt(array[4]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month-1);
        calendar.set(Calendar.DAY_OF_MONTH, date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        long futureTimeInMillis = calendar.getTimeInMillis();

        Calendar now = Calendar.getInstance();
        long nowTimeInMillis = now.getTimeInMillis();

       /*
        //For Testing Only
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY MM DD HH:mm:ss.SSS");
        String test = sdf.format(now.getTime());
        Log.e("Now    :", test);
        test = sdf.format(calendar.getTime());
        Log.e("Future :", test);
        */

        long scheduleTimeInMillis = futureTimeInMillis - nowTimeInMillis;;

      /*  Log.e("Future Time:", Long.toString(futureTimeInMillis));
        Log.e("Now Time   :", Long.toString(nowTimeInMillis));
        System.out.println("Schedule Time :" + Long.toString(scheduleTimeInMillis/60000) + " minutes");
        */

        Intent myIntent = new Intent(EventMonitor.this, EventReceiver.class);

        //Request Code is the number of device in the list
        //Which implies that it will be different for each device
        pendingIntent = PendingIntent.getBroadcast(EventMonitor.this, 0, myIntent, 0);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, futureTimeInMillis, pendingIntent);
        Log.e("EventMonitor", "Latest Alarm for " + mac + " has been scheduled");

        /*
        boolean alarmUp = (PendingIntent.getBroadcast(EventMonitor.this, requestCode, myIntent, PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            Log.e("EventMonitor", "Alarm is already active for " + macAddress);
        } else {
        }
        */

        //alarmManager.cancel(pendingIntent);
        //Log.d("MyActivity", "EventAdd Off");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
