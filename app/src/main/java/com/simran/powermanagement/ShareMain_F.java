package com.simran.powermanagement;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShareMain_F extends Fragment {

    public static List<Device> deviceList = new ArrayList<>();
    private RecyclerView recyclerView;
    public static DeviceAdapter mAdapter;
    FragmentActivity listener;

    Button sendBtn, recvBtn;
    View view;

    View.OnClickListener sendClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent myInt = new Intent(getActivity(), ShareSend.class);
            startActivity(myInt);

        }
    };

    View.OnClickListener recvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent myInt = new Intent(getActivity(), ShareReceive.class);
            startActivity(myInt);

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        view = inflater.inflate(R.layout.activity_share_main, parent, false);
        deviceList.clear();
        Log.d("Share Page", "called");

        sendBtn = view.findViewById(R.id.sendBtn);
        recvBtn = view.findViewById(R.id.recvBtn);
        sendBtn.setOnClickListener(sendClickListener);
        recvBtn.setOnClickListener(recvClickListener);

        //Read MacAddresses from shared.txt Then add them to devicesList
        FileHelper.readMacShared();

        recyclerView = view.findViewById(R.id.recyclerShareView);

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
                ShareActivity.currentMACForSharedOnOff = device.getMacAddress();
                MqttShared.mac = device.getMacAddress();

                Intent deviceIntent = new Intent(getActivity(), ShareActivity.class);
                startActivity(deviceIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

                dialog("Attention!!!", "Do you want to delete the device ?", position);

            }
        }));
        return view;
    }

    public void dialog(String title, String message, int position) {
        android.app.AlertDialog.Builder builder;
        builder = new android.app.AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Device device = deviceList.get(position);

                        int line = FileHelper.returnLineNumber(device.getMacAddress(), FileHelper.sharedDevices);
                        try {
                            FileHelper.removeLine(FileHelper.sharedDevices, line-1);
                            FileHelper.removeLine(FileHelper.sharedDevices, line-1);
                            FileHelper.removeLine(FileHelper.sharedDevices, line-1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Intent mainPageIntent = new Intent(getActivity(), ShareMain_F.class);
                        mainPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainPageIntent);
                    }
                })

                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void setDeviceList(String a, String b, String c) {
        Device device;
        device = new Device(a, b, c);
        deviceList.add(device);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        //Destroy MqttShared
        Log.e("SelfDestruction", "ShareMain_F will be destroyed");
        Intent service = new Intent(getActivity(), MqttShared.class);
        getActivity().stopService(service);

        Intent myInt = new Intent(getActivity(), MainPage.class);
        myInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(myInt);

        this.listener = null;
    }

}
