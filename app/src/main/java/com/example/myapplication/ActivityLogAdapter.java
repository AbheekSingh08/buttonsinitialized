package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class ActivityLogAdapter extends ArrayAdapter<String> {

    private Context context;
    private ArrayList<String> logs;

    public ActivityLogAdapter(Context context, ArrayList<String> logs) {
        super(context, 0, logs);
        this.context = context;
        this.logs = logs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_log_item, parent, false);
        }

        String log = logs.get(position);
        TextView logTextView = convertView.findViewById(R.id.activity_log_text);
        ImageView fileIcon = convertView.findViewById(R.id.file_icon);

        logTextView.setText(log);

        // Highlight the text based on the action
        if (log.contains("Added:")) {
            logTextView.setTextColor(Color.GREEN);
        } else if (log.contains("Deleted:")) {
            logTextView.setTextColor(Color.RED);
        } else {
            logTextView.setTextColor(Color.BLACK);
        }

        // Set the icon based on the file type
        if (log.contains(".zip")) {
            fileIcon.setImageResource(R.drawable.ic_folder); // Use your folder icon resource
        } else if (log.contains(".pdf")) {
            fileIcon.setImageResource(R.drawable.ic_other); // Use your PDF icon resource
        } else if (log.contains(".jpg") || log.contains(".jpeg") || log.contains(".png")) {
            String[] logParts = log.split("\\|");
            String filePath = logParts[0].split(":")[1].trim(); // Get the file path from the log
            File file = new File(context.getFilesDir(), "watch_directory/" + filePath);
            if (file.exists()) {
                fileIcon.setImageURI(Uri.fromFile(file));
            } else {
                fileIcon.setImageResource(R.drawable.ic_other); // Default icon if the file doesn't exist
            }
        } else {
            fileIcon.setImageResource(R.drawable.ic_other); // Use your "Other" icon resource
        }

        return convertView;
    }
}
