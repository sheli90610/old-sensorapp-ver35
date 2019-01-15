package com.simran.powermanagement;

public class Device {

    private String deviceName, macAddress, icon;

    public Device(String deviceName, String macAddress, String icon) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.icon = icon;

    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIcon (){
        return icon;
    }

    public void setIcon(String icon){
        this.icon = icon;
    }
}