package com.simran.powermanagement;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TemperatureMain extends AppCompatActivity {

    protected static String currentMACForTemp;
    TextView tempView, timeView;
    EditText minTemp;
    EditText maxTemp;
    ImageView saveTempLimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_main);
        //To save Temperature limits
        minTemp = findViewById(R.id.minTempView);
        maxTemp = findViewById(R.id.maxTempView);
        //To view Current Temperature
        tempView = findViewById(R.id.tempView);
        timeView = findViewById(R.id.timeView);

        SharedPreferences prefs;
        prefs = getSharedPreferences("myTemp" + currentMACForTemp , MODE_PRIVATE);
        String minTemperature = prefs.getString("minTemp", null);
        String maxTemperature = prefs.getString("maxTemp", null);
        String temperature = prefs.getString("currentTemp", null);
        String time = prefs.getString("currentTempTime", null);



        if (temperature == null) {
            temperature = "N/A";
        }
        if (minTemperature == null) {
            minTemperature = "N/A";
        }
        if (maxTemperature == null) {
            maxTemperature = "N/A";
        }
        if (time == null) {
            time = "N/A";
        }
        tempView.setText(temperature);
        minTemp.setText(minTemperature);
        maxTemp.setText(maxTemperature);
        timeView.setText(time);

        saveTempLimit = findViewById(R.id.saveTempLimitImg);
        saveTempLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(minTemp.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter Minimum limit", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(maxTemp.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter Maximum limit", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(Float.parseFloat(minTemp.getText().toString()) >= Float.parseFloat(maxTemp.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Minimum limit cannot be greater or equal to Maximum limit", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = getSharedPreferences("myTemp" + currentMACForTemp , MODE_PRIVATE).edit();
                editor.putString("minTemp", minTemp.getText().toString());
                editor.putString("maxTemp", maxTemp.getText().toString());
                editor.apply();

                Intent intent = new Intent(getApplicationContext(), DeviceFunctionSelect.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }


    /*
     final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //subs();
            }
        }, 5000);
    */

    /*
     runOnUiThread(new Runnable() {
            @Override
          public void run() {

        }
      });
      */
}
