package com.simran.powermanagement;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.youruserpools.UserActivity;

import java.util.List;

import static com.simran.powermanagement.TempApplication.CHANNEL_ID;


public class Simran_FGService extends Service {

    public static boolean IS_SERVICE_RUNNING = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");

        //This is Sticky
        if (!Simran_FGService.IS_SERVICE_RUNNING) {

            Intent notificationIntent = new Intent(this, MainPage.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Service Management")
                    .setContentText(input)
                    .setSmallIcon(R.drawable.ic_bulb)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
            Simran_FGService.IS_SERVICE_RUNNING = true;
        }

        //This is not sticky
        if (!DDBMain.IS_SERVICE_RUNNING) {
            UserActivity.ddbMainStarted();
            Intent myService = new Intent(getApplicationContext(), DDBMain.class);
            getApplicationContext().startService(myService);
        }

        //This is Sticky
        if (!Mqtt.IS_SERVICE_RUNNING) {
            Intent service = new Intent(getApplicationContext(), Mqtt.class);
            getApplicationContext().startService(service);
            Mqtt.IS_SERVICE_RUNNING = true;
        }
        //This is Not Sticky
        if (!EventMonitor.IS_SERVICE_RUNNING) {
            Intent service = new Intent(getApplicationContext(), EventMonitor.class);
            getApplicationContext().startService(service);
            EventMonitor.IS_SERVICE_RUNNING = true;
        }
        return START_STICKY;
    }

    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}