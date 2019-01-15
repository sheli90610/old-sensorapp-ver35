package com.simran.powermanagement;


import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TempApplication extends Application {

    private static Context context;
    public static final String CHANNEL_ID = "ServiceManagementServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        TempApplication.context = getApplicationContext();
       // JobManager.create(this).addJobCreator(new TempJobCreator());
        createNotificationChannel();
    }

    public static Context getAppContext() {
        return TempApplication.context;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Example Service Channel", NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}