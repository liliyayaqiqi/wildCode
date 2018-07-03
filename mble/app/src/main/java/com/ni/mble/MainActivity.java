package com.ni.mble;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protocted void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Toggle Start Stop Scan
            }
        });
    }
}
