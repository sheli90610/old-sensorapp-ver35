package com.simran.powermanagement;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;


public class EventReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        //this will update the UI with message
        EventAdd inst = EventAdd.instance();
       // inst.setAlarmText("EventAdd! Wake up! Wake up!");

        //this will send a notification message
        ComponentName comp = new ComponentName(context.getPackageName(), EventService.class.getName());
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
