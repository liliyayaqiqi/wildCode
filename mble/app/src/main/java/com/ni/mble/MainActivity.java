package com.ni.mble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS = 2;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView sensorsListView;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;
    private Handler handler;
    private Runnable runnable;
    // Adapter for holding devices found through scanning.
    // TODO: move to its own class
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            class ViewHolder {
                TextView deviceName;
                TextView deviceAddress;
            }

            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.sensor, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_sensor);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;

        }
    }

    private LeDeviceListAdapter leDeviceListAdapter;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leDeviceListAdapter.addDevice(device);
                            leDeviceListAdapter.notifyDataSetChanged();
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

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                toggleScan();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        tryGainPermissions();

        leDeviceListAdapter = new LeDeviceListAdapter();
        sensorsListView.setAdapter(leDeviceListAdapter);

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
            isScanning = false;
            stopScan();
        } else
        {
            isScanning = true;
            startScan();
        }
    }

    private void stopScan() {
        handler.removeCallbacks(runnable);
        bluetoothAdapter.stopLeScan(leScanCallback);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void startScan() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    isScanning = false;
                    stopScan();
                }
            }
        };
        handler.postDelayed(runnable, 10000); // TODO: Need to be configured;
        bluetoothAdapter.startLeScan(leScanCallback);
    }
}
