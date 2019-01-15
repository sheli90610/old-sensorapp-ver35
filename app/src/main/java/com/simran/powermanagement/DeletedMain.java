package com.simran.powermanagement;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeletedMain extends AppCompatActivity {

    public static List<Device> deviceList = new ArrayList<>();
    public static DeviceAdapter mAdapter;
    private RecyclerView recyclerView;
    ImageView addBtn;
    TextView delDeviceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceList.clear();
        Log.d("DeletedMain", "onCreate(Bundle) called");
        setContentView(R.layout.activity_event_main);                             //Opens up the main view

        addBtn = findViewById(R.id.addBtn);
        addBtn.setVisibility(View.GONE);

        //Read MacAddresses from devicesMac.txt Then add them to devicesList
        FileHelper.readMacDeleted();

        if(deviceList.isEmpty()) {
            delDeviceView = findViewById(R.id.delDeviceView);
            delDeviceView.setVisibility(View.VISIBLE);
        }

        recyclerView = findViewById(R.id.recycler_view);

        mAdapter = new DeviceAdapter(deviceList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addItemDecoration(new ItemDividerDecoration(this));
        // set the adapter
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                Device device = deviceList.get(position);

                //Open up the graphs
                Log.d("MAC address of Device", device.getMacAddress());

                int line = FileHelper.returnLineNumber(device.getMacAddress(), FileHelper.deletedDevices);
                String mac = FileHelper.readLine(FileHelper.deletedDevices, line-1);
                String name = FileHelper.readLine(FileHelper.deletedDevices, line);
                String icon = FileHelper.readLine(FileHelper.deletedDevices, line+1);

                Log.e("names", mac + " " + name + " " + icon);

                try {
                    FileHelper.removeLine(FileHelper.deletedDevices, line-1);
                    FileHelper.removeLine(FileHelper.deletedDevices, line-1);
                    FileHelper.removeLine(FileHelper.deletedDevices, line-1);

                    FileHelper.saveToFile(mac, FileHelper.mainDevices);
                    FileHelper.saveToFile(name,FileHelper.mainDevices);
                    FileHelper.saveToFile(icon, FileHelper.mainDevices);

                    Toast.makeText(getApplicationContext(), device.getDeviceName() + " has been restored", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent mainPageIntent = new Intent(DeletedMain.this, MainPage.class);
                mainPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainPageIntent);
            }

            @Override
            public void onLongClick(View view, int position) {
                Device device = deviceList.get(position);

                DeviceEdit.deviceName = device.getDeviceName();
                DeviceEdit.currentMACForIcon = device.getMacAddress();

                Intent deviceEditIntent = new Intent(DeletedMain.this, DeviceEdit.class);
                startActivity(deviceEditIntent);
            }
        }));
    }

    public void setDeviceList(String a, String b, String c) {
        Device device;
        device = new Device(a, b, c);
        deviceList.add(device);
    }
}
