package com.ni.mble;


import android.content.SharedPreferences;

import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;

import android.support.v7.app.ActionBar;
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

import java.util.HashSet;
import java.util.Set;

public class LocationActivity extends AppCompatActivity {
    private ListView locationView;
    private LocationAdapter locationAdapter;

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
        private LocationInfo locationInfos[];

        public LocationAdapter(LocationInfo locationInfos[]) {
            super();
            inflater = LocationActivity.this.getLayoutInflater();
            this.locationInfos = locationInfos;
        }

        public void clear(){
            locationInfos = null;
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


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LocationActivity.this);
            int greenRssi = Integer.parseInt(prefs.getString("green_rssi", LocationActivity.this.getString(R.string.default_green_rssi)));
            int yellowRssi = Integer.parseInt(prefs.getString("yellow_rssi", LocationActivity.this.getString(R.string.default_yellow_rssi)));

            int rssi = Integer.parseInt(info.averageRssi);
            int yellowNum = Integer.parseInt(info.yellowNum);
            int redNum = Integer.parseInt(info.redNum);
            if(rssi >= greenRssi && yellowNum == 0 && redNum == 0){
                int colors[] = {0xffffffff, 0xffffffff, 0xffffffff, 0x9f22bb55 };
                GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                view.setBackground(gd);
            }
            else if (rssi >= yellowRssi && redNum == 0){
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        setTitle(R.string.title_locations);
        SharedPreferences allLocations = getSharedPreferences("locations", 0);
        Set<String> locationInfoNamesSet = allLocations.getStringSet("locations1", new HashSet<String>());
        String[] locationInfoNames = locationInfoNamesSet.toArray(new String[locationInfoNamesSet.size()]);
        LocationInfo[] locationList = new LocationInfo[locationInfoNamesSet.size()];
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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_delete_all_locations:
                deleteLocations();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void deleteLocations() {
        locationAdapter.clear();
        locationAdapter.notifyDataSetChanged();
        SharedPreferences allLocations = getSharedPreferences("locations", 0);
        SharedPreferences.Editor editor = allLocations.edit();
        editor.clear();
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_menu, menu);
        return true;
    }
}
