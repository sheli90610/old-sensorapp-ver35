package com.simran.powermanagement;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private List<Device> deviceList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName, macAddress;
        public ImageView setIcn;

        public MyViewHolder(View view){
            super(view);
            setIcn = view.findViewById(R.id.tvIcon);
            deviceName = view.findViewById(R.id.deviceName);
            macAddress = view.findViewById(R.id.macAddress);
        }
    }


    public DeviceAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.deviceName.setText(device.getDeviceName());
        holder.macAddress.setText(device.getMacAddress());
        holder.setIcn.setImageResource(Integer.parseInt(device.getIcon()));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}