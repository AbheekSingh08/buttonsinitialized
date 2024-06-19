package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WatchDirectoryActivity extends AppCompatActivity {
    public static final String ACTION_UPDATE_WATCH_DIRECTORY = "com.example.myapplication.UPDATE_WATCH_DIRECTORY";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String TRANSMITTED_COUNT_KEY = "transmitted_count";
    private static final String UPLOAD_TIME_KEY_PREFIX = "upload_time_";
    private static final String ACTION_UPDATE_TRANSMITTED_COUNT = "com.example.myapplication.UPDATE_TRANSMITTED_COUNT";
    private static final int PICK_ZIP_FILE_REQUEST = 3;

    private ListView listView;
    private ArrayList<File> mediaFiles;
    private MediaAdapter adapter;
    private SharedPreferences sharedPreferences;

    private BroadcastReceiver watchDirectoryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newFilePath = intent.getStringExtra("newFile");
            if (newFilePath != null) {
                File newFile = new File(newFilePath);
                mediaFiles.add(0, newFile); // Add new file to the top of the list
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_directory);

        // Request write permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        listView = findViewById(R.id.listView);
        mediaFiles = getWatchDirectoryFiles();
        adapter = new MediaAdapter(this, mediaFiles);
        listView.setAdapter(adapter);

        // Register BroadcastReceiver
        registerReceiver(watchDirectoryReceiver, new IntentFilter(ACTION_UPDATE_WATCH_DIRECTORY));

        Button uploadZipButton = findViewById(R.id.button_upload_zip);
        uploadZipButton.setOnClickListener(v -> openZipFilePicker());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister BroadcastReceiver
        unregisterReceiver(watchDirectoryReceiver);
    }

    private void openZipFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        startActivityForResult(intent, PICK_ZIP_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ZIP_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedZipUri = data.getData();
            if (selectedZipUri != null) {
                saveZipToWatchDirectory(selectedZipUri);
            }
        }
    }

    private void saveZipToWatchDirectory(Uri zipUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(zipUri);
            File appDir = new File(getFilesDir(), "watch_directory");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            String fileName = getFileName(zipUri);
            if (fileName == null) {
                Toast.makeText(this, "Failed to get ZIP file name", Toast.LENGTH_SHORT).show();
                return;
            }

            File zipFile = new File(appDir, fileName);
            OutputStream outputStream = new FileOutputStream(zipFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString(UPLOAD_TIME_KEY_PREFIX + zipFile.getName(), currentTime);
            editor.apply();

            Intent broadcastIntent = new Intent(ACTION_UPDATE_WATCH_DIRECTORY);
            broadcastIntent.putExtra("newFile", zipFile.getAbsolutePath());
            sendBroadcast(broadcastIntent);

            Toast.makeText(this, "ZIP file saved successfully to Watch Directory", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save ZIP file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
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
            TextView uploadTime = convertView.findViewById(R.id.upload_time);
            Button viewButton = convertView.findViewById(R.id.button_view);
            Button deleteButton = convertView.findViewById(R.id.button_delete);
            Button transmitButton = convertView.findViewById(R.id.button_transmit);

            fileName.setText(file.getName());
            thumbnail.setImageURI(Uri.fromFile(file));
            String uploadTimeString = sharedPreferences.getString(UPLOAD_TIME_KEY_PREFIX + file.getName(), "Unknown");
            uploadTime.setText(uploadTimeString);

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
                    Toast.makeText(context, "File transmitted", Toast.LENGTH_SHORT).show();

                    // Update the transmitted files count in SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    int transmittedCount = prefs.getInt(TRANSMITTED_COUNT_KEY, 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(TRANSMITTED_COUNT_KEY, transmittedCount + 1);
                    editor.apply();

                    // Send broadcast to update the counter in MainActivity
                    Intent broadcastIntent = new Intent(ACTION_UPDATE_TRANSMITTED_COUNT);
                    context.sendBroadcast(broadcastIntent);
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
