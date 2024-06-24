package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipViewActivity extends AppCompatActivity {

    private ListView zipListView;
    private ArrayList<String> fileNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_zip_view);

        zipListView = findViewById(R.id.zipListView);

        String zipFilePath = getIntent().getStringExtra("zipFilePath");
        if (zipFilePath != null) {
            try {
                displayZipContents(zipFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error reading ZIP file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to get ZIP file path", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayZipContents(String zipFilePath) throws IOException {
        fileNames = new ArrayList<>();
        ZipFile zipFile = new ZipFile(new File(zipFilePath));

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            fileNames.add(entry.getName());
        }
        zipFile.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        zipListView.setAdapter(adapter);
    }
}
