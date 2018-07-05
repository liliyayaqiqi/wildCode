package com.ni.mble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

import java.sql.Time;
import java.sql.Timestamp;

public class MainActivity extends AppCompatActivity{
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS = 2;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView sensorsListView;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;
    private boolean isSuspended = false;
    private Handler handler;
    private Runnable runnable;
    private BleService bleService;
    private SensorListAdapter sensorListAdapter;
    private SnGattReceiver snGattReceiver;
    private int scanPeriod;

    private long scanStopTarget;

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            snGattReceiver.setService(bleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            snGattReceiver.setService(null);
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(Sensor.isDeviceOfInterest(scanRecord)) {
                                Sensor sensor = new Sensor(device, rssi);
                                Log.v(TAG, "device found " + device.getAddress() + " "  + String.valueOf(rssi));
                                sensor = sensorListAdapter.addSensor(sensor);
                                if(sensor.getSn().equals(Sensor.UNKNOW_SN) && bleService != null && snGattReceiver != null) {
                                    snGattReceiver.startReadingSn(sensor.getAddress());
                                }
                                sensorListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        scanPeriod = Integer.parseInt(prefs.getString("scan_period", getString(R.string.default_scan_period)));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        sensorsListView = findViewById(R.id.sensors);

        sensorListAdapter = new SensorListAdapter(this);
        sensorsListView.setAdapter(sensorListAdapter);
        snGattReceiver = new SnGattReceiver(sensorListAdapter, this);

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
            case R.id.menu_delete_all:
                sensorListAdapter.clear();
                sensorListAdapter.notifyDataSetChanged();
                SharedPreferences shareData = getSharedPreferences("devices", 0);
                SharedPreferences.Editor editor = shareData.edit();
                editor.clear();
                editor.commit();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(snGattReceiver, makeGattUpdateIntentFilter());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        scanPeriod = Integer.parseInt(prefs.getString("scan_period", getString(R.string.default_scan_period)));

        tryGainPermissions();



    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isScanning) {
            stopScan();
        }
        unregisterReceiver(snGattReceiver);
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
            startScan();
        }
    }

    public void suspendScan() {
        if (!isSuspended && isScanning) {
            bluetoothAdapter.stopLeScan(leScanCallback);
            isSuspended = true;
            Log.v(TAG, "Scan suspended");
        }
    }

    public void resumeScan() {
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        scanStopTarget = currentTime.getTime() + scanPeriod * 1000;
        if (isScanning
                && isSuspended == true
                && currentTime.getTime() + 3000 < scanStopTarget) {
            bluetoothAdapter.startLeScan(leScanCallback);
            Log.v(TAG, "Scan resumed");
            isSuspended = false;
        }
    }

    private void stopScan() {
        isScanning = false;
        handler.removeCallbacks(runnable);
        bluetoothAdapter.stopLeScan(leScanCallback);
        swipeRefreshLayout.setRefreshing(false);
        snGattReceiver.resetReceiver();
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
            Log.v(TAG, sensor.getSn());
            Log.v(TAG, sensor.getAddress());
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
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        scanStopTarget = currentTime.getTime() + scanPeriod * 1000;
        handler.postDelayed(runnable, scanPeriod * 1000); // TODO: Need to be configured;
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_GATT_SN_READ);
        return intentFilter;
    }
}
