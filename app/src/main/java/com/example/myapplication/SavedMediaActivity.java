package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class SavedMediaActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> mediaList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_media);

        listView = findViewById(R.id.listView);
        mediaList = new ArrayList<>();

        File appDir = new File(getFilesDir(), "saved_media");
        if (appDir.exists()) {
            File[] files = appDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    mediaList.add(file.getName());
                }
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mediaList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedMedia = mediaList.get(position);
                File mediaFile = new File(appDir, selectedMedia);
                Uri uri = Uri.fromFile(mediaFile);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                String mimeType = getMimeType(uri);
                intent.setDataAndType(uri, mimeType);
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedMedia = mediaList.get(position);
                File mediaFile = new File(appDir, selectedMedia);
                if (mediaFile.delete()) {
                    mediaList.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(SavedMediaActivity.this, "Media deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SavedMediaActivity.this, "Failed to delete media", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private String getMimeType(Uri uri) {
        String extension = getFileExtension(uri);
        return extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : "application/octet-stream";
    }

    private String getFileExtension(Uri uri) {
        String extension;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        return extension;
    }
}
