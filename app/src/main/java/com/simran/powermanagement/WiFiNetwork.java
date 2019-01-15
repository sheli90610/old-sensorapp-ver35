package com.simran.powermanagement;

public class WiFiNetwork {

    private String wifiName, capabilities, level;

    public WiFiNetwork(String wifiName, String capabilities, String level) {
        this.wifiName = wifiName;
        this.level = level;
        this.capabilities = capabilities;
    }



    public String getWifiName() {
        return wifiName;
    }
    public String getLevel() {
        return level;
    }
    public String getCapabilities() {
        return capabilities;
    }

    public void setWifiName(String name) {
        this.wifiName = name;
    }
    public void setLevel(String macAddress) {
        this.level = level;
    }
    public void setCapabilities(String macAddress) {
        this.capabilities = capabilities;
    }





}