package com.ni.mble;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;

// Adapter for holding devices found through scanning.
class SensorListAdapter extends BaseAdapter {
    private MainActivity mainActivity;
    private final ArrayList<Sensor> mViewData;
    private Map<String, Sensor> mSensors;
    private LayoutInflater mInflator;

    public SensorListAdapter(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
        mSensors = new HashMap<>();
        mViewData = new ArrayList<>();
        mInflator = mainActivity.getLayoutInflater();
    }

    public void resetScan() {
        for (Sensor sensor : mViewData) {
            sensor.resetScan();
        }
    }
    public Sensor addSensor(Sensor sensor) {
        if(!mSensors.containsKey(sensor.getAddress())) {
            //sensor.setSn("11-22-33-44");
            mSensors.put(sensor.getAddress(), sensor);
            mViewData.add(sensor);
            return sensor;
        }
        else {
            Sensor sensorItem = mSensors.get(sensor.getAddress());
            Log.v("Sensor List Adapter", "rssi " + String.valueOf(sensor.getRssi()));
            sensorItem.updateRssi(sensor.getRssi());
            return sensorItem;
        }
    }

    public Map<String, Sensor> getSensors() {
        return mSensors;
    }

    public void clear() {
        mViewData.clear();
        mSensors.clear();
    }

    @Override
    public int getCount() {
        return mViewData.size();
    }

    @Override
    public Object getItem(int i) {
        return mViewData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        class ViewHolder {
            TextView deviceModule;
            TextView deviceAddress;
            TextView deviceSn;
            TextView deviceRssi;
            TextView timeStamp;
        }

        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.sensor, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = view.findViewById(R.id.device_address);
            viewHolder.deviceModule = view.findViewById(R.id.device_module);
            viewHolder.deviceRssi = view.findViewById(R.id.device_rssi);
            viewHolder.deviceSn = view.findViewById(R.id.device_sn);
            viewHolder.timeStamp = view.findViewById(R.id.timeStamp);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Sensor sensor = mViewData.get(i);
        final String deviceName = sensor.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceModule.setText(mainActivity.getString(R.string.module_title) + deviceName);
        else
            viewHolder.deviceModule.setText(mainActivity.getString(R.string.module_title) + R.string.unknown_sensor);
        viewHolder.deviceAddress.setText(mainActivity.getString(R.string.addr_title) + sensor.getAddress());
        viewHolder.deviceRssi.setText(String.valueOf(sensor.getRssi()) + mainActivity.getString(R.string.rssi_unit));
        String sn = sensor.getSn();
        if (sn.equals(Sensor.UNKNOW_SN))
            sn = mainActivity.getString(R.string.sn_title) + " Polling...";
        else
            sn = mainActivity.getString(R.string.sn_title) + sn;
        viewHolder.deviceSn.setText(sn);
        viewHolder.timeStamp.setText(sensor.getTimeStamp());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        int greenRssi = Integer.parseInt(prefs.getString("green_rssi", mainActivity.getString(R.string.default_green_rssi)));
        int yellowRssi = Integer.parseInt(prefs.getString("yellow_rssi", mainActivity.getString(R.string.default_yellow_rssi)));

        int rssi = sensor.getRssi();
        if(rssi >= greenRssi){
            int colors[] = {0xffffffff, 0xffffffff, 0xffffffff, 0x9f22bb55 };
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            view.setBackground(gd);
        }
        else if (rssi >= yellowRssi){
            int colors[] = {0xffffffff, 0xffffffff, 0xffffffff, 0x9fbbbb22 };
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            view.setBackground(gd);
        }
        else{
            int colors[] = {0xffffffff, 0xffffffff, 0xffffffff, 0xffbb2222 };
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            view.setBackground(gd);
        }

        return view;

    }
}
