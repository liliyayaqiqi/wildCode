package com.ni.mble;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LocationActivity extends AppCompatActivity {
    private ListView locationView;

    class LocationAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public LocationAdapter() {
            super();
            inflater = LocationActivity.this.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int i) {
            return null;
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

            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        locationView = findViewById(R.id.locations);
        locationView.setAdapter(new LocationAdapter());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
