package com.ni.mble;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class WaveformActivity extends AppCompatActivity {

    private WaveformView waveformView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waveform);
/*
        waveformView = findViewById(R.id.waveform);
        double raw[] = new double[10000];
        for (int i = 0; i < raw.length; ++i) {
            int v = i % 100;
            raw[i] = (double)v / 100.0;
        }
        waveformView.updateVisualizer(raw);
        */
    }
}
