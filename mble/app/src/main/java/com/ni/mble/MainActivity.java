package com.ni.mble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import android.content.SharedPreferences;


import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS = 2;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView sensorsListView;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;
    private Handler handler;
    private Runnable runnable;
    private BleService bleService;
    private SensorListAdapter sensorListAdapter;

    private class GattUpdateReceiver extends BroadcastReceiver {
        private SensorListAdapter sensorList;
        private BleService service;
        private String sensorRequestingSn;

        public GattUpdateReceiver(SensorListAdapter sensorList) {
            this.sensorList = sensorList;
            service = null;
            sensorRequestingSn = null;
        }
        public void setService(BleService service) {
            this.service = service;
        }

        public void setSensorRequestingSn(String address) {
            sensorRequestingSn = address;
        }

        private boolean readSn() {
            if(service != null) {
                List<BluetoothGattService> serviceList = service.getSupportedGattServices();
                for(BluetoothGattService srv : serviceList) {
                    if(srv.getUuid().toString().equals(GattAttributes.NI_MBLE_AUX_SERVICE)) {
                        Log.v(TAG, "aux service found");
                        UUID snUuid = UUID.fromString(GattAttributes.NI_MBLE_SN_READ);
                        BluetoothGattCharacteristic characteristic = srv.getCharacteristic(snUuid);
                        if(characteristic != null)
                        {
                            service.readCharacteristic(characteristic);
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
                if(sensorRequestingSn != null
                        && sensorRequestingSn.equals(intent.getStringExtra(BleService.SENSOR_ADDRESS))) {
                    Log.v(TAG,"Start gatt read to poll sn");
                    if(!readSn())
                    {
                        sensorRequestingSn = null;
                    }
                }
            } else if (BleService.ACTION_GATT_SN_READ.equals(action)) {
                Log.v(TAG,"SN is read");
                if(sensorRequestingSn != null
                        && sensorRequestingSn.equals(intent.getStringExtra(BleService.SENSOR_ADDRESS))) {
                    Log.v(TAG,"Start gatt read to poll sn");
                    Sensor sensor = sensorList.getSensors().get(sensorRequestingSn);
                    if(sensor != null) {
                        sensor.setSn(intent.getStringExtra(BleService.SENSOR_SN));
                    }
                    if(service != null) {
                        service.disconnect();
                    }
                    sensorRequestingSn = null;
                }
            }
        }
    }

    private final GattUpdateReceiver gattUpdateReceiver = new GattUpdateReceiver(this.sensorListAdapter);

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            gattUpdateReceiver.setService(bleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            gattUpdateReceiver.setService(null);
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Sensor sensor = new Sensor(device, rssi);
                            sensorListAdapter.addSensor(sensor);
                            if(sensor.getSn() == null) {
                                if(bleService.connect(sensor.getAddress())) {
                                    gattUpdateReceiver.setSensorRequestingSn(sensor.getAddress());
                                }
                            }
                            sensorListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        sensorsListView = findViewById(R.id.sensors);
        sensorListAdapter = new SensorListAdapter(this);
        sensorsListView.setAdapter(sensorListAdapter);
        handler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothManager == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences shareData = getSharedPreferences("devices", 0);
        int i = 0;
        while(true)
        {
            String data = shareData.getString(String.valueOf(i), "Null");
            if (data != "Null")
            {
                String serial_num = data.substring(0, 11);
                String mac_addr = data.substring(11, 28);
                String device_name = data.substring(28);
                if (device_name.equalsIgnoreCase("null"))
                {
                    device_name = "Unknown Sensor";
                }
                Sensor sensor = new Sensor(device_name, mac_addr, serial_num);
                sensorListAdapter.addSensor(sensor);
                sensorListAdapter.notifyDataSetChanged();
            }
            else
            {
                break;
            }
            ++i;
        }



        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                toggleScan();
            }
        });

        Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class){};
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        tryGainPermissions();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isScanning) {
            stopScan();
        }
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }

    private void tryGainPermissions() {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // In real life most of bluetooth LE devices associated with location, so without this
        // permission the sample shows nothing in most cases
        int permissionCoarse = Build.VERSION.SDK_INT >= 23 ?
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) : PackageManager.PERMISSION_GRANTED;
        if (permissionCoarse != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void toggleScan() {
        if (isScanning) {
            stopScan();
        } else
        {
            /*SharedPreferences shareData = getSharedPreferences("devices", 0);
            SharedPreferences.Editor editor = shareData.edit();
            editor.clear();
            editor.commit();*/
            startScan();
        }
    }

    private void stopScan() {
        isScanning = false;
        handler.removeCallbacks(runnable);
        bluetoothAdapter.stopLeScan(leScanCallback);
        swipeRefreshLayout.setRefreshing(false);

        SharedPreferences shareData = getSharedPreferences("devices", 0);
        SharedPreferences.Editor editor = shareData.edit();
        editor.clear();
        editor.commit();
        int sensor_count = sensorListAdapter.getCount();
        for (int i = 0; i < sensor_count; ++i)
        {
            Sensor sensor = (Sensor) sensorListAdapter.getItem(i);
            String data = sensor.getSn() + sensor.getAddress() + sensor.getName();
            editor.putString(String.valueOf(i), data);
            editor.commit();
        }
    }

    private void startScan() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    stopScan();
                }
            }
        };
        isScanning = true;
        handler.postDelayed(runnable, 10000); // TODO: Need to be configured;
        bluetoothAdapter.startLeScan(leScanCallback);
    }
}