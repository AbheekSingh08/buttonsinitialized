package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ViewHashActivity extends AppCompatActivity {

    private static final String TAG = "ViewHashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_hash);

        TextView hashTextView = findViewById(R.id.hash_text_view);

        String hashFileUri = getIntent().getStringExtra("hashFileUri");
        if (hashFileUri != null) {
            try {
                Uri fileUri = Uri.parse(hashFileUri);
                File file = new File(fileUri.getPath());
                if (!file.exists()) {
                    file = new File(fileUri.getPath().replace("/external_files/", Environment.getExternalStorageDirectory() + "/"));
                }
                if (file.exists()) {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    hashTextView.setText(stringBuilder.toString());
                } else {
                    hashTextView.setText("File does not exist: " + file.getAbsolutePath());
                    Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                hashTextView.setText("Failed to load hash file");
                Log.e(TAG, "Failed to load hash file", e);
                e.printStackTrace();
            }
        } else {
            hashTextView.setText("No hash file provided");
            Log.e(TAG, "No hash file provided");
        }
    }
}
