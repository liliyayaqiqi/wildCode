package com.ni.mble;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WaveformActivity extends AppCompatActivity {
    private final static String TAG = WaveformActivity.class.getSimpleName();

    private ListView channelListView;
    private ProgressBar progressBar;
    private String sn;
    private String address;

    private BleService bleService;
    private WaveformReceiver waveformReceiver;
    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            waveformReceiver.setBleService(bleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            waveformReceiver.setBleService(null);
        }
    };

    class WaveformAdapter extends BaseAdapter {
        private double samples[][] = null;
        private LayoutInflater inflater;

        public WaveformAdapter() {
            super();
            inflater = WaveformActivity.this.getLayoutInflater();
        }

        public void updateSamples(double samples[][]) {
            this.samples = samples;
        }

        @Override
        public int getCount() {
            if (samples == null) {
                return 0;
            }
            return samples.length;
        }

        @Override
        public Object getItem(int i) {
            if (samples == null) {
                return null;
            }
            return samples[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            class ViewHolder {
                TextView channelName;
                TextView channelMax;
                TextView channelMin;
                WaveformView waveformView;
            }

            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = inflater.inflate(R.layout.channel, null);
                viewHolder = new ViewHolder();
                viewHolder.channelName = view.findViewById(R.id.channel_name);
                viewHolder.channelMax = view.findViewById(R.id.channel_max);
                viewHolder.channelMin = view.findViewById(R.id.channel_min);
                viewHolder.waveformView = view.findViewById(R.id.channel_waveform);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final String channelName = getString(R.string.channel_name) + " " + String.valueOf(i);
            viewHolder.channelName.setText(channelName);
            double channelSamples[] = samples[i];
            if (samples[i].length > 10000) {
                channelSamples =Arrays.copyOfRange(samples[i], 10000, samples[i].length);
            }

            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;
            for (int s = 0; s < channelSamples.length; ++s) {
                min = Math.min(min, channelSamples[s]);
                max = Math.max(max, channelSamples[s]);
            }

            double lastValue = Double.POSITIVE_INFINITY;
            for (int s = 0; s < channelSamples.length; ++s) {
                if (lastValue != Double.POSITIVE_INFINITY) {
                    if (Math.abs(channelSamples[s] - lastValue) >= 0.5 * (max - min)) {
                        channelSamples[s] = lastValue;
                    }
                }
                lastValue = channelSamples[s];
            }

            max = -Double.MAX_VALUE;
            min = Double.MAX_VALUE;
            for (int s = 0; s < channelSamples.length; ++s) {
                min = Math.min(min, channelSamples[s]);
                max = Math.max(max, channelSamples[s]);
            }

            final String channelMax = getString(R.string.channel_max) + " " + String.format("%4.2e", max) + getString(R.string.sample_unit);
            final String channelMin = getString(R.string.channel_min) + " " + String.format("%4.2e", min) + getString(R.string.sample_unit);

            viewHolder.channelMax.setText(channelMax);
            viewHolder.channelMin.setText(channelMin);
            viewHolder.waveformView.updateVisualizer(channelSamples, min, max);

            return view;
        }
    }
    WaveformAdapter waveformAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waveform);
        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.DEVICE_MAC_ID);
        sn = intent.getStringExtra(MainActivity.DEVICE_SN_ID);
        setTitle(getString(R.string.sn_title) + " " + sn);
        waveformReceiver = new WaveformReceiver(address, this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        channelListView = findViewById(R.id.waveform_channels);
        waveformAdapter = new WaveformAdapter();
        channelListView.setAdapter(waveformAdapter);

        double samples[][] = new double[12][];
        //Random rand = new Random();
        for (int i = 0; i < samples.length; ++i) {
            double channelSamples[] = new double[10000];
            samples[i] = channelSamples;
            for (int j = 0; j < channelSamples.length; ++j) {
                channelSamples[j] = 0;
            }
        }
        waveformAdapter.updateSamples(samples);
        waveformAdapter.notifyDataSetChanged();

        progressBar = findViewById(R.id.acquire_progress);
        progressBar.setVisibility(View.INVISIBLE);

        Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(waveformReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(waveformReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_acquire_waveforme:
                startAcquireWaveform();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateSamples(double[][] samples) {
        waveformAdapter.updateSamples(samples);
        waveformAdapter.notifyDataSetChanged();
        stopAcquireWaveform();
    }

    public void updateProgress(int progress) {
        progressBar.setProgress(progress);
        waveformAdapter.notifyDataSetChanged();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_GATT_CONFIGURED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    private void startAcquireWaveform() {
        progressBar.setVisibility(View.VISIBLE);
        bleService.connect(address);
    }

    private void stopAcquireWaveform() {
        progressBar.setVisibility(View.INVISIBLE);
        bleService.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.waveform_menu, menu);
        return true;
    }

}
