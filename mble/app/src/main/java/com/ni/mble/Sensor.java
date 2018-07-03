package com.ni.mble;

import android.bluetooth.BluetoothDevice;

class Sensor {
    private BluetoothDevice mDevice;
    private int mRssi;
    private String mSn;

    public Sensor(final BluetoothDevice device, int rssi) {
        mDevice = device;
        mRssi = rssi;
        mSn = null;
    }

    public String getName() {
        return mDevice.getName();
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
        return mDevice.getAddress();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
