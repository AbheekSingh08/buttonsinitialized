package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class WatchActivityLogActivity extends AppCompatActivity {

    private ListView activityLogListView;
    private ArrayList<String> activityLogs;
    private ArrayList<String> filteredLogs;
    private ActivityLogAdapter adapter;
    private boolean isNewestToOldest = true;
    private Button toggleSortButton;
    private Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_activity);

        activityLogListView = findViewById(R.id.activity_log_list);
        toggleSortButton = findViewById(R.id.button_toggle_sort);
        filterSpinner = findViewById(R.id.spinner_filters);

        // Retrieve the activity logs from SharedPreferences
        activityLogs = new ArrayList<>();
        filteredLogs = new ArrayList<>();
        loadActivityLogs();

        filteredLogs.addAll(activityLogs); // Initially show all logs
        adapter = new ActivityLogAdapter(this, filteredLogs);
        activityLogListView.setAdapter(adapter);

        toggleSortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNewestToOldest = !isNewestToOldest;
                toggleSortButton.setText(isNewestToOldest ? "Sort: Oldest to Newest" : "Sort: Newest to Oldest");
                Collections.reverse(filteredLogs);
                adapter.notifyDataSetChanged();
            }
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = parent.getItemAtPosition(position).toString();
                applyFilter(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadActivityLogs() {
        SharedPreferences prefs = getSharedPreferences("ActivityLogs", MODE_PRIVATE);
        int logCount = prefs.getInt("log_count", 0);

        for (int i = logCount - 1; i >= 0; i--) {
            String log = prefs.getString("log_" + i, "");
            activityLogs.add(log);
        }

        // Initially sort as Newest to Oldest
        Collections.reverse(activityLogs);
    }

    private void applyFilter(String filter) {
        filteredLogs.clear();

        for (String log : activityLogs) {
            if (filter.equals("Added") && log.contains("Added:")) {
                filteredLogs.add(log);
            } else if (filter.equals("Deleted") && log.contains("Deleted:")) {
                filteredLogs.add(log);
            } else if (filter.equals("Transmitted") && log.contains("Transmitted:")) {
                filteredLogs.add(log);
            } else if (filter.equals("Last Five Minutes") && logWithinLastFiveMinutes(log)) {
                filteredLogs.add(log);
            } else if (filter.equals("Image/Video") && (log.contains(".jpg") || log.contains(".jpeg") || log.contains(".png") || log.contains(".mp4"))) {
                filteredLogs.add(log);
            } else if (filter.equals("Zip File") && log.contains(".zip")) {
                filteredLogs.add(log);
            } else if (filter.equals("Other File") && !(log.contains(".jpg") || log.contains(".jpeg") || log.contains(".png") || log.contains(".mp4") || log.contains(".zip"))) {
                filteredLogs.add(log);
            } else if (filter.equals("Filters")) { // Show all logs
                filteredLogs.addAll(activityLogs);
                break;
            }
        }

        adapter.notifyDataSetChanged();
    }

    private boolean logWithinLastFiveMinutes(String log) {
        // Extract the timestamp from the log entry
        String timestampPattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(timestampPattern, Locale.getDefault());

        // Assuming the log format includes the timestamp like this: "Added: filename | Time: 2024-06-24 12:34:56"
        // Extract the time part from the log
        int timeIndex = log.indexOf("| Time: ");
        if (timeIndex != -1) {
            String timestamp = log.substring(timeIndex + 8).trim(); // Extract the timestamp part

            try {
                Date logDate = sdf.parse(timestamp);
                Date currentDate = new Date();

                // Calculate the time difference in milliseconds
                long difference = currentDate.getTime() - logDate.getTime();

                // Check if the difference is within 5 minutes (5 * 60 * 1000 milliseconds)
                return difference <= 5 * 60 * 1000;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
