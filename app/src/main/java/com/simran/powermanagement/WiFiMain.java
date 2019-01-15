package com.simran.powermanagement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WiFiMain extends AppCompatActivity {

    public static List<WiFiNetwork> wifiList = new ArrayList<WiFiNetwork>();
    public static WiFiScanAdapter mAdapter = new WiFiScanAdapter(wifiList);
    private WifiManager wifiManager;
    private Button buttonManScan;
    private List<ScanResult> results;
    private RecyclerView recyclerView;
    private ProgressBar simpleProgressBar;
    private String TAG = "WiFi";

    public String getCurrentSsid(Context context) {
        String ssid = null;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            ssid = info.getExtraInfo();
        }

        return ssid;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_main);

        buttonManScan = findViewById(R.id.manScanBtn);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Enabling WiFi...", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);
        simpleProgressBar.setVisibility(View.INVISIBLE);
        //prefView = findViewById(R.id.prefView);

        scanWifi();

        buttonManScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                final WiFiNetwork wifi = wifiList.get(position);
                final String SSID = wifi.getWifiName();

                String wifiMAC = SSID.substring(11);
                dialog("Attention!!!", "Do you really want to add device to your list ?", wifiMAC);
                connectWiFi(wifi.getWifiName(), "", wifi.getCapabilities());
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    public void dialog(String title, String message, String mac) {
        android.app.AlertDialog.Builder builder;
        builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        //For saving to file
                        int i = 0;
                        FileHelper.createFileIfNotThere(FileHelper.mainDevices);                                     //Creates devicesMac file if it doesn't exist.
                        if (!(FileHelper.readString(mac, FileHelper.mainDevices))) {    //If devicesMac.txt file doesnot contain that MacAddress. Then,
                            FileHelper.saveToFile(mac, FileHelper.mainDevices);       //Save the MacAddress to devicesMac.txt which contains all Mac Addresses
                            FileHelper.saveToFile("XXSimranXX" + mac, FileHelper.mainDevices);       //Save the Name to devicesMac.txt which contains all Mac Addresses
                            FileHelper.saveToFile("2131230830", FileHelper.mainDevices);       //Save the icon to devicesMac.txt which contains all Mac Addresses
                        }

                        simpleProgressBar.setVisibility(View.VISIBLE);

                        String ssid = getCurrentSsid(WiFiMain.this);

                        Intent myInt = new Intent(WiFiMain.this, WebActivity.class);
                        startActivity(myInt);
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

    public void connectWiFi(String SSID, String password, String Security) {
        try {
            Log.d(TAG, "Item clicked, SSID " + SSID + " Security : " + Security);
            String networkSSID = SSID;
            String networkPass = password;
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;
            // Check if security type is WEP
            if (Security.toUpperCase().contains("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                if (networkPass.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = networkPass;
                } else {
                    conf.wepKeys[0] = "\"".concat(networkPass).concat("\"");
                }
                conf.wepTxKeyIndex = 0;
                // Check if security type is WPA
            } else if (Security.toUpperCase().contains("WPA")) {
                Log.v(TAG, "Configuring WPA");
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                conf.preSharedKey = "\"" + networkPass + "\"";
                // Check if network is open network
            } else {
                Log.v(TAG, "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }
            //Connect to the network
            WifiManager wifiManager = (WifiManager) WiFiMain.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int networkId = wifiManager.addNetwork(conf);
            Log.v(TAG, "Add result " + networkId);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    Log.v(TAG, "WifiConfiguration SSID " + i.SSID);
                    boolean isDisconnected = wifiManager.disconnect();
                    Log.v(TAG, "isDisconnected : " + isDisconnected);
                    boolean isEnabled = wifiManager.enableNetwork(i.networkId, true);
                    Log.v(TAG, "isEnabled : " + isEnabled);
                    boolean isReconnected = wifiManager.reconnect();
                    Log.v(TAG, "isReconnected : " + isReconnected);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanWifi() {

        boolean mobileDataEnabled = false; //Assume disabled
        ConnectivityManager cm = (ConnectivityManager) (WiFiMain.this).getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
        }

        if (!mobileDataEnabled) {

            buttonManScan.setEnabled(false);
            buttonManScan.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            simpleProgressBar.setVisibility(View.VISIBLE);

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                (TempApplication.getAppContext()).registerReceiver(wifiReceiverOther, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            }else{
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.net.wifi.SCAN_RESULTS");
                (TempApplication.getAppContext()).registerReceiver(new wifiReceiver(), intentFilter);
            }

            wifiManager.startScan();
            Toast.makeText(this, "Finding new devices...", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "Please Disable your Mobile Data", Toast.LENGTH_SHORT).show();
        }
    }

    public class wifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            simpleProgressBar.setVisibility(View.INVISIBLE);
            results = wifiManager.getScanResults();

            wifiList.clear();

            for (ScanResult scanResult : results) {
                WiFiNetwork wifiNetwork;
                //Show All of the networks of PowerMeter
                if (scanResult.SSID.contains("PowerMeter@")) {
                    wifiNetwork = new WiFiNetwork(scanResult.SSID, scanResult.capabilities, Integer.toString(-1 * scanResult.level) + "%");
                    wifiList.add(wifiNetwork);
                }
                mAdapter.notifyDataSetChanged();
            }
            buttonManScan.setEnabled(true);
            buttonManScan.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        }
    }

    BroadcastReceiver wifiReceiverOther = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            simpleProgressBar.setVisibility(View.INVISIBLE);
            results = wifiManager.getScanResults();

            wifiList.clear();

            for (ScanResult scanResult : results) {
                WiFiNetwork wifiNetwork;
                //Show All of the networks of PowerMeter
                if (scanResult.SSID.contains("PowerMeter@")) {
                    wifiNetwork = new WiFiNetwork(scanResult.SSID, scanResult.capabilities, Integer.toString(-1 * scanResult.level) + "%");
                    wifiList.add(wifiNetwork);
                }
                mAdapter.notifyDataSetChanged();
            }
            buttonManScan.setEnabled(true);
            buttonManScan.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));

        }
    };

}

