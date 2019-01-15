package com.simran.powermanagement;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DeviceMain_F extends Fragment {
    public static List<Device> deviceList = new ArrayList<>();
    private RecyclerView recyclerView;
    public static DeviceAdapter mAdapter;

    ImageView addBtn;
    View view;

    View.OnClickListener addClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            dialog("Add device ?", "Would you like to add restore deleted device ?");

        }
    };

    public void dialog(String title, String message) {
        android.app.AlertDialog.Builder builder;
        builder = new android.app.AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent myInt = new Intent(getActivity(), DeletedMain.class);
                        startActivity(myInt);
                    }
                })


                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent myInt = new Intent(getActivity(), WiFiMain.class);
                        startActivity(myInt);
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_event_main, parent, false);
        Log.d("Device Page", "called");

        deviceList.clear();

        addBtn = view.findViewById(R.id.addBtn);
        addBtn.setOnClickListener(addClickListener);

        //Read MacAddresses from devicesMac.txt Then add them to devicesList
        FileHelper.readMac();

        recyclerView = view.findViewById(R.id.recycler_view);

        mAdapter = new DeviceAdapter(deviceList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addItemDecoration(new ItemDividerDecoration(getActivity()));
        // set the adapter
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                Device device = deviceList.get(position);
                //Toast.makeText(getApplicationContext(), device.getDeviceName() + " Power Usage", Toast.LENGTH_SHORT).show();

                //Open up the graphs
                Log.d("MAC address of Device",device.getMacAddress());
                GraphMain.currentMACForGraph = device.getMacAddress();
                TemperatureMain.currentMACForTemp = device.getMacAddress();
                EventAdd.currentMACForScheduling = device.getMacAddress();
                EventMain.currentMACForEventDisplay = device.getMacAddress();
                DeviceFunctionSelect.currentMACForONOff = device.getMacAddress();

                Intent deviceIntent = new Intent(getActivity(), DeviceFunctionSelect.class);
                startActivity(deviceIntent);
            }

            @Override
            public void onLongClick(View view, int position) {
                Device device = deviceList.get(position);

                DeviceEdit.deviceName = device.getDeviceName();
                DeviceEdit.currentMACForIcon = device.getMacAddress();

                Intent deviceEditIntent = new Intent(getActivity(), DeviceEdit.class);
                startActivity(deviceEditIntent);
            }
        }));

        return view;
    }

    public void setDeviceList(String a, String b, String c) {
        Device device;
        device = new Device(a, b, c);
        deviceList.add(device);
    }

}
