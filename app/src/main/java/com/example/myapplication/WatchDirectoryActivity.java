package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class WatchDirectoryActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<File> mediaFiles;
    private MediaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_media);

        // Request write permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        listView = findViewById(R.id.listView);
        mediaFiles = getWatchDirectoryFiles();
        adapter = new MediaAdapter(this, mediaFiles);
        listView.setAdapter(adapter);
    }

    private ArrayList<File> getWatchDirectoryFiles() {
        ArrayList<File> files = new ArrayList<>();
        File appDir = new File(getFilesDir(), "watch_directory");
        if (appDir.exists() && appDir.isDirectory()) {
            for (File file : appDir.listFiles()) {
                files.add(file);
            }
        }
        return files;
    }

    private class MediaAdapter extends ArrayAdapter<File> {
        private Context context;
        private ArrayList<File> files;

        public MediaAdapter(Context context, ArrayList<File> files) {
            super(context, 0, files);
            this.context = context;
            this.files = files;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.media_item_watch_directory, parent, false);
            }
            File file = files.get(position);
            TextView fileName = convertView.findViewById(R.id.file_name);
            ImageView thumbnail = convertView.findViewById(R.id.thumbnail);
            Button viewButton = convertView.findViewById(R.id.button_view);
            Button deleteButton = convertView.findViewById(R.id.button_delete);
            Button transmitButton = convertView.findViewById(R.id.button_transmit);

            fileName.setText(file.getName());
            thumbnail.setImageURI(Uri.fromFile(file));

            if (file.getName().endsWith(".sha.txt")) {
                thumbnail.setVisibility(View.GONE);
                viewButton.setText("View Hash");
                viewButton.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ViewHashActivity.class);
                    Uri fileUri = FileProvider.getUriForFile(context, "com.example.myapplication.fileprovider", file);
                    intent.putExtra("hashFileUri", fileUri.toString());
                    context.startActivity(intent);
                });
            } else {
                thumbnail.setVisibility(View.VISIBLE);
                viewButton.setText("View");
                viewButton.setOnClickListener(v -> {
                    Intent intent = new Intent(context, FullScreenMediaActivity.class);
                    Uri fileUri = FileProvider.getUriForFile(context, "com.example.myapplication.fileprovider", file);
                    intent.putExtra("mediaUrl", fileUri.toString());
                    String fileType = getFileType(file);
                    if (fileType.startsWith("image")) {
                        intent.putExtra("mediaType", "image");
                    } else if (fileType.startsWith("video")) {
                        intent.putExtra("mediaType", "video");
                    }
                    context.startActivity(intent);
                });
            }

            deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Confirmation")
                        .setMessage("Are you sure you want to delete this file?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (file.delete()) {
                                files.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            });

            transmitButton.setOnClickListener(v -> {
                try {
                    File transmittedDir = new File(getFilesDir(), "saved_media");
                    if (!transmittedDir.exists()) {
                        transmittedDir.mkdirs();
                    }
                    File transmittedFile = new File(transmittedDir, file.getName());
                    copyFile(file, transmittedFile);
                    files.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "File transmitted", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(context, "Failed to transmit file", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });

            return convertView;
        }

        private String getFileType(File file) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        private void copyFile(File src, File dst) throws IOException {
            try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }
}