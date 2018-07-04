package com.ni.mble;

import android.bluetooth.BluetoothDevice;

class Sensor {
    private BluetoothDevice mDevice;
    private int mRssi;
    private String mName;
    private String mAddress;
    private String mSn;

    public Sensor(final BluetoothDevice device, int rssi) {
        mDevice = device;
        mRssi = rssi;
        mName = device.getName();
        mAddress = device.getAddress();
        mSn = null;
    }

    public Sensor(String name, String address, String Sn) {
        mDevice = null;
        mRssi = -80;
        mName = name;
        mAddress = address;
        mSn = Sn;
    }

    public String getName() {
        return mName;
    }

    public int getRssi() {
        return mRssi;
    }

    public void updateRssi(int rssi) {
        mRssi = rssi;
    }

    public String getSn() { return mSn; }

    public void setSn(String Sn) {
        mSn = Sn;
    }

    public String getAddress() {
        return mAddress;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
