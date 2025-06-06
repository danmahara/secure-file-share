package com.securefileshare.models;

public class PeerDevice {
    private String deviceName;
    private String deviceAddress;

    public PeerDevice(String name, String address) {
        this.deviceName = name;
        this.deviceAddress = address;
    }

    public String getDeviceName() { return deviceName; }
    public String getDeviceAddress() { return deviceAddress; }

    @Override
    public String toString() {
        return deviceName + " (" + deviceAddress + ")";
    }
}
