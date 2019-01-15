package com.simran.powermanagement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            String input = "Monitoring Devices";
            Intent serviceIntent = new Intent(context, Simran_FGService.class);
            serviceIntent.putExtra("inputExtra", input);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}