package com.ni.mble;

import android.bluetooth.BluetoothDevice;

import java.text.SimpleDateFormat;
import java.util.Locale;

import java.sql.Timestamp;
import java.time.Instant;

class Sensor {
    public final static String UNKNOW_SN = "00 00 00 00";
    private BluetoothDevice mDevice;
    private int mTotalRssi = 0;
    private int mTotalScans = 0;
    private int mRssi;
    private String mName;
    private String mAddress;
    private String mSn;
    private String mTimeStamp;

    public void resetScan() {
        mTotalRssi = 0;
        mTotalScans = 0;
    }

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
        mTimeStamp = getTime();
    }

    public Sensor(String name, String address, String Sn) {
        mDevice = null;
        mRssi = -80;
        mName = name;
        mAddress = address;
        mSn = Sn;
        mTimeStamp = getTime();
    }

    public Sensor(String name, String address, String Sn, String time, String rssi) {
        mDevice = null;
        mRssi = Integer.parseInt(rssi);
        mName = name;
        mAddress = address;
        mSn = Sn;
        mTimeStamp = time;
    }

    public String getName() {
        return mName;
    }

    public int getRssi() {
        return mRssi;
    }

    public void updateRssi(int rssi) {
        ++mTotalScans;
        mTotalRssi += rssi;
        mRssi = mTotalRssi / mTotalScans;
        mTimeStamp = getTime();
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

    public String getTimeStamp(){
        return mTimeStamp;
    }
    private String getTime(){
        SimpleDateFormat dataFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return dataFormat.format(new java.util.Date(System.currentTimeMillis()));
    }
}
