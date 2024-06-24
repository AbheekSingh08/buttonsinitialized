package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class WatchActivityLogActivity extends AppCompatActivity {

    private ListView activityLogListView;
    private ArrayList<String> activityLogs;
    private ActivityLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_activity);

        activityLogListView = findViewById(R.id.activity_log_list);

        // Retrieve the activity logs from SharedPreferences
        activityLogs = new ArrayList<>();
        loadActivityLogs();

        adapter = new ActivityLogAdapter(this, activityLogs);
        activityLogListView.setAdapter(adapter);
    }

    private void loadActivityLogs() {
        SharedPreferences prefs = getSharedPreferences("ActivityLogs", MODE_PRIVATE);
        int logCount = prefs.getInt("log_count", 0);

        for (int i = logCount - 1; i >= 0; i--) {
            String log = prefs.getString("log_" + i, "");
            activityLogs.add(log);
        }
    }
}
