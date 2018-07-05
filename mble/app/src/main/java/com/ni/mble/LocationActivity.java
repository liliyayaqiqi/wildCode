package com.ni.mble;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

public class LocationActivity extends AppCompatActivity {

    class locationInfo {
        String averageRssi;
        String scannedNum;
        String greenNum;
        String yellowNum;
        String redNum;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        String[] locationInfoNames = getIntent().getStringArrayExtra("locationInfoNames");
        locationInfo[] locationList = new locationInfo[locationInfoNames.length];
        for (int i = 0; i < locationInfoNames.length; ++i)
        {
            String fileName = locationInfoNames[i];
            SharedPreferences locationData = getSharedPreferences(fileName, 0);
            locationInfo myLocation = new locationInfo();
            myLocation.averageRssi = locationData.getString("average_rssi", "Null");
            myLocation.scannedNum = locationData.getString("scanned_num", "Null");
            myLocation.greenNum = locationData.getString("green_num", "Null");
            myLocation.yellowNum = locationData.getString("yellow_num", "Null");
            myLocation.redNum = locationData.getString("red_num", "Null");
            locationList[i] = myLocation;
        }
    }
}
