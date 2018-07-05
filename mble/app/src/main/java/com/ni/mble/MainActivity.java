package com.ni.mble;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS = 2;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView sensorsListView;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;
    private Handler handler;
    private Runnable runnable;
    private int scanPeriod;

    private int greenNum = 0;
    private int yellowNum = 0;
    private int redNum = 0;
    private int scannedNum = 0;
    private int averageRssi = 0;
    private Set<String> locationInfo = new HashSet<String>();

    private AlertDialog alertDialog;

    private SensorListAdapter sensorListAdapter;
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
                                sensorListAdapter.addSensor(sensor);
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
                return true;
            case R.id.menu_save_location:
                if (sensorListAdapter.getCount()>0)
                {
                    initDialog();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        updateLocationInfo();
    }

    private void saveLocation(String locationName)
    {
        SharedPreferences shareData = getSharedPreferences(locationName, 0);
        SharedPreferences.Editor editor = shareData.edit();
        if(locationInfo.contains(locationName))
        {
            editor.clear();
            editor.commit();
        }
        else
        {
            locationInfo.add(locationName);
        }
        editor.putString("average_rssi", String.valueOf(averageRssi));
        editor.putString("scanned_num", String.valueOf(scannedNum));
        editor.putString("green_num", String.valueOf(greenNum));
        editor.putString("yellow_num", String.valueOf(yellowNum));
        editor.putString("red_num", String.valueOf(redNum));
        editor.commit();
    }

    private void updateLocationInfo() {

        int sensor_count = sensorListAdapter.getCount();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int greenRssi = Integer.parseInt(prefs.getString("green_rssi", this.getString(R.string.default_green_rssi)));
        int yellowRssi = Integer.parseInt(prefs.getString("yellow_rssi", this.getString(R.string.default_yellow_rssi)));
        int totalRssi = 0;
        resetLocationInfo();
        for (int i = 0; i < sensor_count; ++i)
        {
            Sensor sensor = (Sensor) sensorListAdapter.getItem(i);
            int rssi = sensor.getRssi();
            totalRssi += rssi;
            if (rssi >= greenRssi)
            {
                ++greenNum;
            }
            else if (rssi >= yellowRssi)
            {
                ++yellowNum;
            }
            else
            {
                ++redNum;
            }
        }
        averageRssi = totalRssi/sensor_count;
        scannedNum = sensor_count;
    }

    private void resetLocationInfo(){
        averageRssi = 0;
        scannedNum = 0;
        greenNum = 0;
        yellowNum = 0;
        redNum = 0;
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
        handler.postDelayed(runnable, scanPeriod * 1000); // TODO: Need to be configured;
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    public  void initDialog()
    {
        final EditText et = new EditText(MainActivity.this);
        et.setText("Location 1");
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Location Info");
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setView(et);
        String average = "Average RSSI: " + String.valueOf(averageRssi);
        String scanned = "Scanned: "+ String.valueOf(scannedNum);
        String green = "Green: "+ String.valueOf(greenNum);
        String yellow = "Yellow: "+ String.valueOf(yellowNum);
        String red = "Red: "+ String.valueOf(redNum);
        final String listItems[] = new String[]{average, scanned, green, yellow, red, "Please enter the location name"};
        builder.setItems(listItems, null);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String input = et.getText().toString();
                Toast.makeText(MainActivity.this, "Saved " + input, Toast.LENGTH_SHORT).show();
                saveLocation(input);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }
}
