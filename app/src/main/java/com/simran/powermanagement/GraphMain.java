package com.simran.powermanagement;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.simran.powermanagement.FileHelper.path;

public class GraphMain extends AppCompatActivity {

    //There will be 5 files
    //devicesMac.txt = Stores Mac Addressess of devices and their associated names
    //xxx"MAC".txt = Stores data of last 24 hours
    //xwx"MAC".txt = Stroes data of last 7 days
    //xxm"MAC.txt = Stores data of last 30 days
    //mmm"MAC".txt = Stores data of last 6 months

    public static String currentMACForGraph;              //*******************needs "xxx"+".txt"
    public static String period;
    static ArrayList<BarEntry> BarEntry = new ArrayList<>();
    static ArrayList<String> labels = new ArrayList<>();
    private static String graph;
    String time;

    String statement = null;

    public class MyValueFormatter implements ValueFormatter {

        private DecimalFormat mFormat;

        public MyValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0.00"); // use one decimal
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            // write your logic here
            return mFormat.format(value); // e.g. append a dollar-sign
        }
    }

    public static void readData() {

        BufferedReader bufferedReader;
        int i = 0;
        try {

            bufferedReader = new BufferedReader(new FileReader(path + graph));
            String line = bufferedReader.readLine();
            while (line != null) {

                //Set  (value, set index)
                BarEntry.add(new BarEntry(Float.parseFloat(line), i));

                //Read next line
                line = bufferedReader.readLine();

                //Set X-axis label
                line = timeZone(line);
                labels.add(line);

                line = bufferedReader.readLine();
                i++;
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String timeZone(String timestamp) {
        long unixSeconds = Long.parseLong(timestamp);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = null;

        // convert seconds to milliseconds
        Date date = new java.util.Date(unixSeconds * 1000L);
        // the format of your date
        // SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        if (period.equals("xxx")) {
            sdf = new SimpleDateFormat("HH:mm");
        } else if (period.equals("xwx")) {
            sdf = new SimpleDateFormat("dd-MM");
        } else if (period.equals("xxm")) {
            sdf = new SimpleDateFormat("dd-MM-yyyy");
        }
        // give a timezone reference for formatting (see comment at the bottom)
        sdf.setTimeZone(tz);
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Graph Main", "onCreate(Bundle) called");
        labels.clear();
        BarEntry.clear();

        graph = period + currentMACForGraph + ".txt";         //Don't put this outside onCreate

        setContentView(R.layout.activity_graph_main);

        BarChart barChart = findViewById(R.id.bargraph);

        readData();

        BarDataSet dataSet = new BarDataSet(BarEntry, statement);
        dataSet.setValueFormatter(new MyValueFormatter());

        BarData data = new BarData(labels, dataSet);


        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        barChart.setData(data);

        barChart.setDescription("Power Usage");

        barChart.animateY(1000);
    }


}
