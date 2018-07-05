package com.ni.mble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class SnGattReceiver extends BroadcastReceiver {
    private final static String TAG = BroadcastReceiver.class.getSimpleName();
    private SensorListAdapter sensorList;
    private BleService bleService;
    private String sensorRequestingSn;

    public SnGattReceiver(SensorListAdapter sensorList) {
        this.sensorList = sensorList;
        bleService = null;
        sensorRequestingSn = null;
    }
    public void setService(BleService service) {
        this.bleService = service;
    }

    public void startReadingSn(String address) {
        Log.v(TAG,"startReadingSn");
        if(sensorRequestingSn != null) {
            Log.v(TAG, "Address not NULL, return");
            return;
        }
        if(bleService != null && bleService.connect(address)) {
            Log.v(TAG, "connect to server success, set address");
            sensorRequestingSn = address;
        } else {
            Log.v(TAG, "connect failed");
        }
    }

    private boolean readSn() {
        if(bleService != null) {
            List<BluetoothGattService> serviceList = bleService.getSupportedGattServices();
            for(BluetoothGattService srv : serviceList) {
                Log.v(TAG, srv.getUuid().toString());
                if(srv.getUuid().toString().equals(GattAttributes.NI_MBLE_AUX_SERVICE)) {
                    Log.v(TAG, "aux service found");
                    UUID snUuid = UUID.fromString(GattAttributes.NI_MBLE_SN_READ);
                    BluetoothGattCharacteristic characteristic = srv.getCharacteristic(snUuid);
                    if(characteristic != null)
                    {
                        bleService.readCharacteristic(characteristic);
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(BleService.ACTION_GATT_CONNECTED.equals(action)) {
            Log.v(TAG,"Connection established");
        } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            Log.v(TAG,"Services discovered");
            Log.v(TAG, intent.getStringExtra(BleService.SENSOR_ADDRESS));
            if(sensorRequestingSn != null
                    && sensorRequestingSn.equals(intent.getStringExtra(BleService.SENSOR_ADDRESS))) {
                Log.v(TAG,"Start gatt read to poll sn");
                if(!readSn())
                {
                    bleService.disconnect();
                }
            }
        } else if (BleService.ACTION_GATT_SN_READ.equals(action)) {
            Log.v(TAG,"SN is read");
            if(sensorRequestingSn != null
                    && sensorRequestingSn.equals(intent.getStringExtra(BleService.SENSOR_ADDRESS))) {
                Log.v(TAG,"Start gatt read to poll sn");
                Log.v(TAG, intent.getStringExtra(BleService.SENSOR_SN));
                Sensor sensor = sensorList.getSensors().get(sensorRequestingSn);
                if(sensor != null) {
                    sensor.setSn(intent.getStringExtra(BleService.SENSOR_SN));
                    sensorList.notifyDataSetChanged();
                }
                if(bleService != null) {
                    Log.v(TAG,"Sn is read, disconnect");
                    bleService.disconnect();
                }
            }
        } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
            Log.v(TAG,"Disconnected, reset sn address");
            sensorRequestingSn = null;
        }
    }
}

