package com.ni.mble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class WaveformReceiver extends BroadcastReceiver {
        private final static String TAG = WaveformReceiver.class.getSimpleName();
        private String address;
        private BleService bleService;
        private WaveformActivity activity;

        private static final int DISCONNECTED = 0;
        private static final int CONNECTED = 1;
        private static final int CONFIGURED = 2;
        private static final int WAITINGDISCONN = 3;
        private static final int RECONNECTED = 4;
        private static final int TRANSFERRING = 5;
        private static final int READY = 6;

        private int currentStatus = DISCONNECTED;

        private final byte[] configData = {0x01,
                (byte)0xFF,
                0x20, 0x00, 0x00, (byte)0xC8, 0x00,
                0x20, 0x55, 0x00, (byte)0xC8, 0x00,
                0x20, (byte)0xAA, 0x00, (byte)0xC8, 0x00,
                0x00, (byte)0xFF, 0x00, 0x00, 0x00,
                0x00, (byte)0xFF, 0x00, 0x00, 0x00,
                0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A, 0x1A};
        private final byte[] configStartCmd = {0x01,
                0x01, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00};
        private final byte[] startTransferCmd = {0x00,
                0x00, 0x00, 0x00, 0x00,
                0x24, 0x20, 0x1C, 0x00,
                0x24, 0x20, 0x1C, 0x00};

        private final int totalByteLen = 1843236;
        private final int samplesPerChann = 51200;
        /*private final byte[] freeBufferCmd = {0x01,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x20, 0x1C, 0x00,
                0x00, 0x20, 0x1C, 0x00};*/

        private double[][] samples = new double[12][samplesPerChann];
        private ByteArrayOutputStream rawData = new ByteArrayOutputStream();

        public WaveformReceiver(String address, WaveformActivity activity) {
            this.address = address;
            this.activity = activity;
            this.bleService = null;
        }

        public WaveformReceiver() {
            address = null;
            activity = null;
            bleService = null;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setBleService(BleService bleService) {
            this.bleService = bleService;
        }

        private void onConnected() {
            if (currentStatus == DISCONNECTED) {
                currentStatus = CONNECTED;
            } else if(currentStatus == WAITINGDISCONN) {
                currentStatus = RECONNECTED;
            }
        }

        private boolean onServiceDiscovered() {
            if (bleService == null) {
                return false;
            }
            List<BluetoothGattService> serviceList = bleService.getSupportedGattServices();
            for(BluetoothGattService srv : serviceList) {
                Log.v(TAG, srv.getUuid().toString());
                if(srv.getUuid().toString().equals(GattAttributes.NI_MBLE_MEAS_SERVICE)) {
                    Log.v(TAG, "meas service found");
                    if (currentStatus == CONNECTED) {
                        UUID configUuid = UUID.fromString(GattAttributes.NI_MBLE_MEAS_CONFIG_WAVEFORM);
                        BluetoothGattCharacteristic characteristic = srv.getCharacteristic(configUuid);
                        characteristic.setValue(configData);
                        bleService.writeCharacteristic(characteristic);
                        //Toast.makeText(activity, "Start configuring waveform acquisition", Toast.LENGTH_SHORT).show();
                        activity.setText("Start configuring waveform acquisition");
                        return true;
                    }
                    if (currentStatus == RECONNECTED) {
                        UUID transDataUuid = UUID.fromString(GattAttributes.NI_MBLE_MEAS_TRANS_DATA);
                        BluetoothGattCharacteristic cccd = srv.getCharacteristic(transDataUuid);
                        bleService.setCharacteristicNotification(cccd, true);
                        ///Ensure cccd is enabled
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        UUID transUuid = UUID.fromString(GattAttributes.NI_MBLE_MEAS_TRANS_CONTROL);
                        BluetoothGattCharacteristic characteristic = srv.getCharacteristic(transUuid);
                        characteristic.setValue(startTransferCmd);
                        bleService.writeCharacteristic(characteristic);
                        //Toast.makeText(activity, "Retrieving waveform data...", Toast.LENGTH_SHORT).show();
                        activity.setText("Retrieving waveform data...");
                        return true;
                    }
                    break;
                }
            }
            return false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BleService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.v(TAG,"Connection established");
                Toast.makeText(activity, "Remote enpoint is successfully connected!", Toast.LENGTH_SHORT).show();
                //activity.setText("Remote enpoint is successfully connected!");
                onConnected();
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.v(TAG,"Services discovered");
                Log.v(TAG, intent.getStringExtra(BleService.SENSOR_ADDRESS));
                if(address != null
                        && address.equals(intent.getStringExtra(BleService.SENSOR_ADDRESS))) {
                    if(!onServiceDiscovered())
                    {
                        bleService.disconnect();
                    }
                }
            } else if (BleService.ACTION_GATT_CONFIGURED.equals(action)) {
                Log.v(TAG,"Gatt is configured");
                Toast.makeText(activity, "Configuration accepted!", Toast.LENGTH_SHORT).show();
                //activity.setText("Configuration accepted!");
                if (bleService != null) {
                    for(BluetoothGattService srv: bleService.getSupportedGattServices()) {
                        if(srv.getUuid().toString().equals(GattAttributes.NI_MBLE_MEAS_SERVICE)) {
                            Log.v(TAG, "meas service found");
                            UUID configStartUuid = UUID.fromString(GattAttributes.NI_MBLE_MEAS_CONFIG_START);
                            BluetoothGattCharacteristic characteristic = srv.getCharacteristic(configStartUuid);
                            characteristic.setValue(configStartCmd);
                            bleService.writeCharacteristic(characteristic);
                            Log.v(TAG,"Gatt Config start is written");
                            //Toast.makeText(activity, "Preparing waveform data...", Toast.LENGTH_SHORT).show();
                            activity.setText("Preparing waveform data...");
                            currentStatus = WAITINGDISCONN;
                            break;
                        }
                    }
                }
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.v(TAG,"Disconnected received!!!");
                if (currentStatus == WAITINGDISCONN) {
                    bleService.connect(address);
                } else {
                    currentStatus = DISCONNECTED;
                }
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BleService.RAW_DATA);
                int offset = 0;
                if (rawData.size() == 0) {
                    offset = 36;
                }
                rawData.write(data, offset, data.length - offset);
                Double percentage = new Double((double)rawData.size() / (double)totalByteLen * (double)100);
                activity.updateProgress(percentage.intValue());
                Log.v(TAG, "current received bytes is " + String.valueOf(rawData.size()));
                Log.v(TAG, "target size is " + String.valueOf(totalByteLen - 36));
                if (rawData.size() == totalByteLen - 36) {
                    byte[] waveformData = rawData.toByteArray();
                    Log.v(TAG, "sample number is " + String.valueOf(waveformData.length / 3));
                    int i = 0;
                    while (i < waveformData.length) {
                        int tmp = 0;
                        for (int j = 0; j < 3; ++j) {
                            tmp += waveformData[i + j] << (8 * j);
                        }
                        if (tmp > 0x8000000) {
                            tmp |= 0xFF000000;
                        }
                        int sampleIndex = i / 3;
                        samples[sampleIndex / samplesPerChann][sampleIndex % samplesPerChann] = (double)tmp * 3.66211e-6;
                        //Log.v(TAG, "sample value is " + String.valueOf(sampleIndex / samplesPerChann) + " " + String.valueOf(sampleIndex % samplesPerChann) + " " + String.valueOf((double)tmp * 3.66211e-6));
                        i += 3;
                    }
                    activity.setText("Waveform data collected!");
                    Toast.makeText(activity, "Waveform data collected!", Toast.LENGTH_SHORT).show();
                    activity.updateSamples(samples);
                    rawData.reset();
                }

            }
        }
}
