package com.ni.mble;

import android.bluetooth.BluetoothDevice;

class Sensor {
    private BluetoothDevice mDevice;
    private int mRssi;
    private String mName;
    private String mAddress;
    private String mSn;
    private boolean mIsRequestingSn;

    public Sensor(final BluetoothDevice device, int rssi) {
        mDevice = device;
        mRssi = rssi;
        mName = device.getName();
        mAddress = device.getAddress();
        mSn = null;
        mIsRequestingSn = false;
    }

    public Sensor(String name, String address, String Sn) {
        mDevice = null;
        mRssi = -80;
        mName = name;
        mAddress = address;
        mSn = Sn;
        mIsRequestingSn = false;
    }

    public String getName() {
        return mName;
    }

    public int getRssi() {
        if(mSn == null)
        {
            mIsRequestingSn = true;
        }
        return mRssi;
    }

    public void updateRssi(int rssi) {
        mRssi = rssi;
    }

    public String getSn() { return mSn; }

    public boolean isRequestingSn() { return mIsRequestingSn; }

    public void setSn(String Sn) {
        mIsRequestingSn = false;
        mSn = Sn;
    }

    public String getAddress() {
        return mAddress;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
