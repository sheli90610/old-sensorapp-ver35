package com.simran.powermanagement;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class WiFiScanAdapter extends RecyclerView.Adapter<WiFiScanAdapter.MyViewHolder> {

    private List<WiFiNetwork> wifiList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView ssid, level;

        public MyViewHolder(View view){
            super(view);
            ssid = view.findViewById(R.id.ssidView);
            level = view.findViewById(R.id.levelView);
        }
    }


    public WiFiScanAdapter(List<WiFiNetwork> wifiList) {
        this.wifiList = wifiList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        WiFiNetwork wifi = wifiList.get(position);
        holder.ssid.setText(wifi.getWifiName());
        holder.level.setText(wifi.getLevel());
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }
}