package com.ni.mble;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.sql.Timestamp;
import java.time.Instant;

class Sensor {
    public final static String UNKNOW_SN = "00 00 00 00";
    private BluetoothDevice mDevice;
    private int mRssi;
    private String mName;
    private String mAddress;
    private String mSn;
    private Timestamp mLastUpdatedTime;

    public static boolean isDeviceOfInterest(byte[] scanRecord) {
        final int MANUFACTURER_DATA = 0xFF;
        int startIdx = 0;
        while(startIdx < scanRecord.length) {
            int record = scanRecord[startIdx] & 0xFF;
            if(record == MANUFACTURER_DATA) {
                if(startIdx + 3 > scanRecord.length) {
                    return false;
                }
                if(scanRecord[startIdx + 1] == 0x54 && scanRecord[startIdx + 2] == 0x05)
                {
                    return true;
                }
            }
            ++startIdx;
        }
        return false;
    }

    public Sensor(final BluetoothDevice device, int rssi) {
        mDevice = device;
        mRssi = rssi;
        mName = device.getName();
        mAddress = device.getAddress();
        mSn = UNKNOW_SN;
        mLastUpdatedTime = new Timestamp(System.currentTimeMillis());
    }

    public Sensor(String name, String address, String Sn) {
        mDevice = null;
        mRssi = -80;
        mName = name;
        mAddress = address;
        mSn = Sn;
        mLastUpdatedTime = new Timestamp(System.currentTimeMillis());
    }

    public Sensor(String name, String address, String Sn, long time) {
        mDevice = null;
        mRssi = -80;
        mName = name;
        mAddress = address;
        mSn = Sn;
        mLastUpdatedTime = new Timestamp(time);
    }

    public long getLatestUpdatedTime() {
        return mLastUpdatedTime.getTime();
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
