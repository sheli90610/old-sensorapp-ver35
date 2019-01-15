package com.simran.powermanagement;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Student Class
class EventDetails {
    String mac, id, state;
    int year, month, date, hour, minute;

    public EventDetails(String mac, String id, String state, int year, int month, int date, int hour, int minute) {
        this.mac = mac;
        this.id = id;
        this.state = state;
        this.year = year;
        this.month = month;
        this.date = date;
        this.hour = hour;
        this.minute = minute;
    }
}

//nameCompare Class to compare the names
class nameCompare implements Comparator<EventDetails> {
    @Override
    public int compare(EventDetails s1, EventDetails s2) {
        return s1.mac.compareTo(s2.mac);
    }
}

//marksCompare Class to compare the marks
class marksCompare implements Comparator<EventDetails> {
    @Override
    public int compare(EventDetails s1, EventDetails s2) {

        //      year,month,date,hour,minute

        //We will convert everything to minutes
        long s1Year = s1.year*525600;
        long s1Month = s1.month*43800;
        long s1Day = s1.date*1440;
        long s1Hour = s1.hour*60;
        long s1Minute = s1.minute;

        long s2Year = s2.year*525600;
        long s2Month = s2.month*43800;
        long s2Day = s2.date*1440;
        long s2Hour = s2.hour*60;
        long s2Minute = s2.minute;

        long s1Value = s1Year+s1Month+s1Day+s1Hour+s1Minute;
        long s2Value = s2Year+s2Month+s2Day+s2Hour+s2Minute;

        Log.e("S1Value", Long.toString(s1Value));
        Log.e("S2Value", Long.toString(s1Value));

        if (s1Value < s2Value) {
            return -1;
        } else if(s1Value > s2Value) {
            return 1;
        } else {
            return 0;
        }
    }
}

public class EventSort {
    public static void main(String fileName) throws IOException {
        //Creating BufferedReader object to read the input text file
        BufferedReader reader = new BufferedReader(new FileReader(FileHelper.path + fileName));

        //Creating ArrayList to hold Student objects
        ArrayList<EventDetails> eventRecords = new ArrayList<EventDetails>();

        //Reading Student records one by one
        String currentLine = reader.readLine();

        while (currentLine != null) {

            String mac = currentLine;

            currentLine = reader.readLine();
            String id = currentLine;

            currentLine = reader.readLine();
            String state = currentLine;

            currentLine = reader.readLine();
            String finalTime = currentLine;

            String[] array = finalTime.split("/");
            int year = Integer.parseInt(array[0]);
            int month = Integer.parseInt(array[1]);
            int date = Integer.parseInt(array[2]);
            int hour = Integer.parseInt(array[3]);
            int minute = Integer.parseInt(array[4]);

            //Creating Student object for every student record and adding it to ArrayList
            eventRecords.add(new EventDetails(mac, id, state, year, month, date, hour, minute));

            currentLine = reader.readLine();
        }

        //Sorting ArrayList eventRecords based on time
        Collections.sort(eventRecords, new marksCompare());

        //Creating BufferedWriter object to write into output text file
        BufferedWriter writer = new BufferedWriter(new FileWriter(FileHelper.path + fileName));

        //Writing every studentRecords into output text file
        for (EventDetails eventDetails : eventRecords) {
            writer.write(eventDetails.mac + "\n");
            writer.write(eventDetails.id + "\n");
            writer.write(eventDetails.state + "\n");
            String finalTime = eventDetails.year + "/" + eventDetails.month + "/" + eventDetails.date + "/" + eventDetails.hour + "/" + eventDetails.minute;
            writer.write(finalTime + "\n");
            // writer.newLine();
        }
        //Closing the resources
        reader.close();
        writer.close();
    }
}