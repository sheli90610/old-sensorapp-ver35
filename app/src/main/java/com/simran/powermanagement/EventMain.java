package com.simran.powermanagement;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class EventMain extends AppCompatActivity {
    public static List<Event> eventList = new ArrayList<>();
    public static EventAdapter mAdapter;
    ImageView addBtn;
    private RecyclerView recyclerView;

    ImageView imgEvent;

    public static  String currentMACForEventDisplay;

    View.OnClickListener addBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent myInt = new Intent(EventMain.this, EventAdd.class);
            startActivity(myInt);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventList.clear();
        Log.d("Event Page", "onCreate(Bundle) called");
        setContentView(R.layout.activity_event_main);                             //Opens up the main view
        imgEvent = findViewById(R.id.stateIcon);

        //Read MacAddresses from devicesMac.txt Then add them to devicesList
        FileHelper.readEvent("sch" + currentMACForEventDisplay + ".txt");

        recyclerView = findViewById(R.id.recycler_view);

        mAdapter = new EventAdapter(eventList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addItemDecoration(new ItemDividerDecoration(this));

        // set the adapter
        recyclerView.setAdapter(mAdapter);

        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(addBtnClickListener);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    public void setEventList(String a, String b, String c) {
        Event event;
        event = new Event(a, b, c);
        eventList.add(event);
        System.out.println("I got a request to add event to RecyclerView");
    }

}
