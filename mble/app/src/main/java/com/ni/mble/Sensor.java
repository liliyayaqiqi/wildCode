package com.ni.mble;

import android.bluetooth.BluetoothDevice;

class Sensor {
    private BluetoothDevice mDevice;
    private int mRssi;
    private String mSn;

    public static boolean isDeviceOfInterest(byte[] scanRecord) {
        final int MANUFACTURER_DATA = 0xFF;
        int startIdx = 0;
        while(startIdx < scanRecord.length) {
            int length = scanRecord[startIdx];
            if(length < 1 || startIdx + length + 1 > scanRecord.length) {
                return false;
            }
            if(scanRecord[startIdx + 1] == MANUFACTURER_DATA) {
                if(length < 3) {
                    return false;
                }
                if(scanRecord[startIdx + 2] == 0x54 && scanRecord[startIdx + 3] == 0x05)
                {
                    return true;
                }
            }
            startIdx += length + 1;
        }
        return false;
    }

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
