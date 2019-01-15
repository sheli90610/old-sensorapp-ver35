package com.simran.powermanagement;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EventAdd extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    public static String currentMACForScheduling;
    private static EventAdd inst;
    // private TimePicker alarmTimePicker;
    private TextView uEventId;

    private ImageView switchImg;
    private Button doneBtn;
    private boolean toggleOn;
    private String state;


    private Calendar calendar = Calendar.getInstance();
    private int startYear;
    private int startMonth;
    private int startDay;
    private String reminderHourOfDay;
    private String reminderMinute;
    private String reminderDate;
    private TextView timeView, dateView;

    View.OnClickListener switchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (!toggleOn) {
                switchImg.setImageResource(R.drawable.toggle_on);
                toggleOn = true;
                state = "Turn On";
            } else {
                switchImg.setImageResource(R.drawable.toggle_off);
                toggleOn = false;
                state = "Turn Off";
            }
        }
    };

    String finalTime;

    View.OnClickListener doneBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d("MyActivity", "EventAdd On");
            if (uEventId.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(), "Please enter a unique identifier for Schedle", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reminderHourOfDay.length() == 1) {
                reminderHourOfDay = "0" + reminderHourOfDay;
            }
            if (reminderMinute.length() == 1) {
                reminderMinute = "0" + reminderMinute;
            }
            finalTime = reminderDate + "/" + reminderHourOfDay + "/" + reminderMinute;

            boolean timeCheck = FileHelper.readString(finalTime, "sch" + currentMACForScheduling + ".txt");

            //boolean timeCheck = FileHelper.readString(reminderHourOfDay + "\n" + reminderMinute + "\n" + reminderDate, "sch" + currentMACForScheduling + ".txt");

            boolean uName = FileHelper.readString(uEventId.getText().toString(), "schs.txt");

            if (timeCheck) {
                Toast.makeText(getApplicationContext(), "Event with exact time already exist for same device", Toast.LENGTH_SHORT).show();
                return;
            }
            if (uName) {
                Toast.makeText(getApplicationContext(), "Name for the event is not unique", Toast.LENGTH_SHORT).show();
                return;
            }

            //Individual files for devices
            FileHelper.saveToFile(currentMACForScheduling, "sch" + currentMACForScheduling + ".txt");                     //This line is not necessary but since we are using same criteria as of schs.txt we need it
            FileHelper.saveToFile(uEventId.getText().toString(), "sch" + currentMACForScheduling + ".txt");
            FileHelper.saveToFile(state, "sch" + currentMACForScheduling + ".txt");
            FileHelper.saveToFile(finalTime, "sch" + currentMACForScheduling + ".txt");

            //Single file for all devices
            FileHelper.saveToFile(currentMACForScheduling, "schs.txt");
            FileHelper.saveToFile(uEventId.getText().toString(), "schs.txt");
            FileHelper.saveToFile(state, "schs.txt");
            FileHelper.saveToFile(finalTime, "schs.txt");

            //Sorting data in text files
            try {
                EventSort.main("sch" + currentMACForScheduling + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                EventSort.main("schs.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }


            Intent service = new Intent(getApplicationContext(), EventMonitor.class);
            getApplicationContext().startService(service);

            Intent myInt = new Intent(getApplicationContext(), EventMain.class);
            myInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(myInt);
        }
    };

    public static EventAdd instance() {
        return inst;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_event_f);

        uEventId = findViewById(R.id.uEventId);

        switchImg = findViewById(R.id.switchImg);
        switchImg.setOnClickListener(switchClickListener);
        doneBtn = findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(doneBtnListener);
        toggleOn = false;
        state = "Turn Off";

        timeView = findViewById(R.id.timeView);
        dateView = findViewById(R.id.dateView);

        initDatePicker();
        initTimePicker();

    }

    private void initTimePicker() {
        SimpleDateFormat hour = new SimpleDateFormat("HH");
        SimpleDateFormat minute = new SimpleDateFormat("mm");
        this.reminderHourOfDay = hour.format(new Date());
        this.reminderMinute = minute.format(new Date());

        this.timeView.setText(CustomDateUtils.getDisplayTime(reminderHourOfDay, reminderMinute));
    }

    private void initDatePicker() {
        this.startYear = calendar.get(Calendar.YEAR);
        this.startMonth = calendar.get(Calendar.MONTH);
        this.startDay = calendar.get(Calendar.DAY_OF_MONTH);
        reminderDate = startYear + "/" + startMonth + 1 + "/" + startDay;

        final String displayedDeadline = CustomDateUtils.getDisplayDate(this, calendar);
        this.dateView.setText(displayedDeadline);
    }


    public void onBack(View view) {
        onBackPressed();
    }

    public void onDatePick(View view) {
        DatePickerDialog dialog = new DatePickerDialog(this, R.style.PickerTheme,this, startYear, startMonth, startDay);

        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        reminderHourOfDay = String.valueOf(hourOfDay);
        if (reminderHourOfDay.length() == 1)
            reminderHourOfDay = "0" + reminderHourOfDay;
        reminderMinute = String.valueOf(minute);
        if (reminderMinute.length() == 1)
            reminderMinute = "0" + reminderMinute;

        calendar.set(startYear, startMonth, startDay);
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(reminderHourOfDay));
        calendar.set(Calendar.MINUTE, Integer.valueOf(reminderMinute));
        timeView.setText(CustomDateUtils.getDisplayTime(reminderHourOfDay, reminderMinute));

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        if (!CustomDateUtils.isBeforeToday(calendar)) {
            startYear = year;
            startMonth = month;
            startDay = dayOfMonth;

            calendar.set(startYear, startMonth, startDay);
            this.calendar.set(startYear, startMonth, startDay);

            reminderDate = year + "/" + month + 1 + "/" + dayOfMonth;

            final String displayedDeadline = CustomDateUtils.getDisplayDate(this, calendar);
            dateView.setText(displayedDeadline);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_create_task_date_picking_too_early), Toast.LENGTH_SHORT).show();
        }
    }

    public void onTimePick(View view) {
        TimePickerDialog dialog = new TimePickerDialog(this, R.style.PickerTheme,this, Integer.valueOf(reminderHourOfDay), Integer.valueOf(reminderMinute), true);
        dialog.show();
    }
}