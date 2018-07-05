package com.ni.mble;


import android.content.SharedPreferences;

import android.support.v4.app.NavUtils;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LocationActivity extends AppCompatActivity {
    private ListView locationView;

    class LocationInfo {
        String name;
        String averageRssi;
        String scannedNum;
        String greenNum;
        String yellowNum;
        String redNum;
    }

    class LocationAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private final LocationInfo locationInfos[];

        public LocationAdapter(LocationInfo locationInfos[]) {
            super();
            inflater = LocationActivity.this.getLayoutInflater();
            this.locationInfos = locationInfos;
        }

        @Override
        public int getCount() {
            if (locationInfos == null) {
                return 0;
            }
            return locationInfos.length;
        }

        @Override
        public Object getItem(int i) {
            return locationInfos[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            class ViewHolder {
                TextView locationName;
                TextView averageRssi;
                TextView summary;
            }

            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = inflater.inflate(R.layout.location, null);
                viewHolder = new ViewHolder();
                viewHolder.locationName = view.findViewById(R.id.location_name);
                viewHolder.summary = view.findViewById(R.id.location_summary);
                viewHolder.averageRssi = view.findViewById(R.id.location_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            LocationInfo info = locationInfos[i];
            viewHolder.locationName.setText(info.name);

            String eol = System.getProperty("line.separator");
            String summary = getString(R.string.location_summary) + eol;
            String indent = new String();
            for (int s = 0; s < summary.length(); ++s) {
                indent += " ";
            }

            summary += indent + info.scannedNum + " " + getString(R.string.location_scanned) + " " + eol;
            summary += indent + info.greenNum + " " + getString(R.string.location_green) + " " + eol;
            summary += indent + info.yellowNum + " " + getString(R.string.location_yellow) + " " + eol;
            summary += indent + info.redNum + " " + getString(R.string.location_red);
            viewHolder.summary.setText(summary);
            viewHolder.averageRssi.setText(getString(R.string.location_rssi) + " " + info.averageRssi);
            return view;

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        setTitle(R.string.title_locations);

        String[] locationInfoNames = getIntent().getStringArrayExtra("locationInfoNames");
        LocationInfo[] locationList = new LocationInfo[locationInfoNames.length];
        for (int i = 0; i < locationInfoNames.length; ++i)
        {
            String fileName = locationInfoNames[i];
            SharedPreferences locationData = getSharedPreferences(fileName, 0);
            LocationInfo myLocation = new LocationInfo();
            myLocation.name = fileName;
            myLocation.averageRssi = locationData.getString("average_rssi", "Null");
            myLocation.scannedNum = locationData.getString("scanned_num", "Null");
            myLocation.greenNum = locationData.getString("green_num", "Null");
            myLocation.yellowNum = locationData.getString("yellow_num", "Null");
            myLocation.redNum = locationData.getString("red_num", "Null");
            locationList[i] = myLocation;
        }

        locationView = findViewById(R.id.locations);
        locationView.setAdapter(new LocationAdapter(locationList));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_delete_all:
                deleteLocations();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void deleteLocations() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_menu, menu);
        return true;
    }
}
