package com.simran.powermanagement;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.simran.powermanagement.GraphMain.currentMACForGraph;

public class GraphSelectPeriod extends AppCompatActivity {

    //There will be 8 files
    //devicesMac.txt = Stores Mac Addressess of devices and their associated names
    //xxx"MAC".txt = Stores data of last 24 hours
    //xwx"MAC".txt = Stroes data of last 7 days
    //xxm"MAC.txt = Stores data of last 30 days
    //mmm"MAC".txt = Stores data of last 6 months

    //Creation of files is handled by DDBMain
    static float powerHigh, powerLow;
    static TextView tPow24, tPow7, tPow30;
    static float tPower24, tPower7, tPower30;
    static TextView tcost24, tcost7, tcost30;
    static TextView highTimeCostView, lowTimeCostView;
    ImageView imgTwentyFour, imgSevenDays, imgThirtyDays;
    Button enterCostBtn;

    View.OnLongClickListener twentyFourHoursListenerLong = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            dialog("24 Hour Power Usage", "Click it to view latest 24 hour power usage");
            return false;
        }
    };

    View.OnLongClickListener sevenDaysListenerLong = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            dialog("7 days Power Usage", "Click it to view latest 7 Days Power Usage");
            return false;
        }
    };

    View.OnLongClickListener thirtyDaysListenerLong = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            dialog("30 days Power Usage", "Click it to view latest 30 Days Power Usage");
            return false;
        }
    };

    public static void processTotal() {

        int numOfItems = FileHelper.countItems("xxx" + currentMACForGraph + ".txt");
        tPower24 = FileHelper.addEveryOtherItem("xxx" + currentMACForGraph + ".txt", numOfItems);
        tPow24.setText(Float.toString(tPower24) + " kW");

        numOfItems = FileHelper.countItems("xwx" + currentMACForGraph + ".txt");
        tPower7 = FileHelper.addEveryOtherItem("xwx" + currentMACForGraph + ".txt", numOfItems);
        tPow7.setText(Float.toString(tPower7) + " kW");

        numOfItems = FileHelper.countItems("xxm" + currentMACForGraph + ".txt");
        tPower30 = FileHelper.addEveryOtherItem("xxm" + currentMACForGraph + ".txt", numOfItems);
        tPow30.setText(Float.toString(tPower30) + " kW");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_period_select);                             //Opens up the main view

        tcost24 = findViewById(R.id.cost24);
        tcost7 = findViewById(R.id.cost7);
        tcost30 = findViewById(R.id.cost30);

        tPower24 = 0;
        tPower7 = 0;
        tPower30 = 0;
        // totalPower += Long.parseLong(line);
        tPow24 = findViewById(R.id.pow24);
        tPow7 = findViewById(R.id.pow7);
        tPow30 = findViewById(R.id.pow30);

        highTimeCostView = findViewById(R.id.highTimeCostView);
        lowTimeCostView = findViewById(R.id.lowTimeCostView);

        //Do not rearrange the order
        processTotal();
        processTime();
        setHighLowCost();

        enterCostBtn = findViewById(R.id.enterCostBtn);
        enterCostBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myInt = new Intent(GraphSelectPeriod.this, GraphCost.class);
                startActivity(myInt);
            }
        });

        imgTwentyFour = findViewById(R.id.twentyFourHoursImg);
        imgTwentyFour.setOnLongClickListener(twentyFourHoursListenerLong);
        imgTwentyFour.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GraphMain.period = "xxx";
                Intent myInt = new Intent(GraphSelectPeriod.this, GraphMain.class);
                startActivity(myInt);

            }
        });

        imgSevenDays = findViewById(R.id.sevenDaysImg);
        imgSevenDays.setOnLongClickListener(sevenDaysListenerLong);
        imgSevenDays.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GraphMain.period = "xwx";
                Intent myInt = new Intent(GraphSelectPeriod.this, GraphMain.class);
                startActivity(myInt);
            }
        });

        imgThirtyDays = findViewById(R.id.thirtyDaysImg);
        imgThirtyDays.setOnLongClickListener(thirtyDaysListenerLong);
        imgThirtyDays.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GraphMain.period = "xxm";
                Intent myInt = new Intent(GraphSelectPeriod.this, GraphMain.class);
                startActivity(myInt);
            }
        });

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

    public void processTime() {
        SharedPreferences prefs;
        prefs = getSharedPreferences("myTime", MODE_PRIVATE);
        String fromHighTime = prefs.getString("fromHighTime", null);
        String toHighTime = prefs.getString("toHighTime", null);
        String fromLowTime = prefs.getString("fromLowTime", null);
        String toLowTime = prefs.getString("toLowTime", null);

        if (fromHighTime == null || toHighTime == null || fromLowTime == null || toLowTime == null) {
            return;
        }

        int numOfItems = FileHelper.countItems("xxx" + currentMACForGraph + ".txt");

        String dataTimeStampMaximum = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", numOfItems - 1);
        String dataTimeStampMinimum = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", 1);

        long dataTimeStampMaximumLong = Long.parseLong(dataTimeStampMaximum);
        long dataTimeStampMinimumLong = Long.parseLong(dataTimeStampMinimum);

        //Long systemTime = System.currentTimeMillis() / 1000;
        //String systemTimeStamp = Long.toString(systemTime);
        //String systemTS1 = processTimeStamp(systemTimeStamp, "yyyy-MM");

        powerHigh = 0;
        powerLow = 0;

        if ((dataTimeStampMaximumLong - dataTimeStampMinimumLong) < 86400) {      //86400 is equal to 24 hours
            int fromHighTimeInt = Integer.parseInt(fromHighTime);
            int toHighTimeInt = Integer.parseInt(toHighTime);

            //Continue this loop
            for (int i = 1; i < numOfItems; i = i + 2) {
                String check = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", i);
                int checked = Integer.parseInt(processTimeStamp(check, "HH"));

                Log.e("PowerHighCheck", check);
                Log.e("PowerHighChecked", Integer.toString(checked));
                //If the value just checked is greater than 'fromHigh' and smaller than 'toLow'
                if (checked >= fromHighTimeInt && checked < toHighTimeInt) {
                    String power = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", i - 1);
                    Log.i("PowerHigh", power);
                    powerHigh += Float.parseFloat(power);
                }
            }

            int fromLowTimeInt = Integer.parseInt(fromLowTime);
            int toLowTimeInt = Integer.parseInt(toLowTime);

            //Continue this loop
            for (int i = 1; i < numOfItems; i = i + 2) {
                String check = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", i);
                int checked = Integer.parseInt(processTimeStamp(check, "HH"));

                Log.e("PowerLowCheck", check);
                Log.e("PowerLowChecked", Integer.toString(checked));
                //If the value just checked is greater than 'fromLow' and smaller than 'toLow'
                if (checked >= fromLowTimeInt || checked < toLowTimeInt) {
                    String power = FileHelper.readLine("xxx" + currentMACForGraph + ".txt", i - 1);
                    Log.i("PowerLow", power);
                    powerLow += Float.parseFloat(power);
                }
            }
        } else {
            Toast.makeText(GraphSelectPeriod.this, "Data is Stale", Toast.LENGTH_LONG).show();
            //Data is stale
            return;
        }
    }

    public void setHighLowCost() {

        Log.e("powerHigh", Float.toString(powerHigh));
        Log.e("powerLow", Float.toString(powerLow));

        SharedPreferences prefs;
        prefs = getSharedPreferences("myCost", MODE_PRIVATE);
        String strCostHigh = prefs.getString("costHigh", null);
        String strCostLow = prefs.getString("costLow", null);

        DecimalFormat twoDPlaces = new DecimalFormat("#.##");

        if (strCostHigh != null && strCostLow != null) {
            Float costHigh = Float.parseFloat(strCostHigh);
            Float costLow = Float.parseFloat(strCostLow);

            String x = Float.toString(Float.valueOf(twoDPlaces.format(costHigh * powerHigh)));
            highTimeCostView.setText("High Cost: $" + x);

            x = Float.toString(Float.valueOf(twoDPlaces.format(costLow * powerLow)));
            lowTimeCostView.setText("Low Cost:  $" + x);

            x = Float.toString(Float.valueOf(twoDPlaces.format( (costHigh * powerHigh)+(costLow * powerLow))));
            tcost24.setText("Total Cost: $" + x);

            x = Float.toString(Float.valueOf(twoDPlaces.format(costHigh * tPower7)));
            tcost7.setText("$" + x);

            x = Float.toString(Float.valueOf(twoDPlaces.format(costHigh * tPower30)));
            tcost30.setText("$" + x);
        }
    }

    public String processTimeStamp(String timeStamp, String pattern) {
        long unixSeconds = Long.parseLong(timeStamp);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = null;


        // convert seconds to milliseconds
        Date date = new java.util.Date(unixSeconds * 1000L);
        // SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(tz);
        return sdf.format(date);
    }

}
