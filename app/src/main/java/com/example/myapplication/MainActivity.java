package com.example.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_VIDEO_REQUEST = 1;
    private static final int CAPTURE_IMAGE_VIDEO_REQUEST = 2;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String TRANSMITTED_COUNT_KEY = "transmitted_count";
    private static final String UPLOAD_TIME_KEY_PREFIX = "upload_time_";
    private static final String ACTION_UPDATE_TRANSMITTED_COUNT = "com.example.myapplication.UPDATE_TRANSMITTED_COUNT";

    private Uri capturedMediaUri;
    private TextView unreadWatchDirectoryCount;
    private TextView unreadTransmittedFilesCount;
    private int watchDirectoryUnreadCount = 0;
    private int transmittedFilesUnreadCount = 0;

    private BroadcastReceiver transmittedCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transmittedFilesUnreadCount++;
            updateUnreadCounts();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        unreadWatchDirectoryCount = findViewById(R.id.unread_watch_directory_count);
        unreadTransmittedFilesCount = findViewById(R.id.unread_transmitted_files_count);

        // Load the transmitted files count from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        transmittedFilesUnreadCount = prefs.getInt(TRANSMITTED_COUNT_KEY, 0);

        // Register BroadcastReceiver
        registerReceiver(transmittedCountReceiver, new IntentFilter(ACTION_UPDATE_TRANSMITTED_COUNT));

        // Button to open the gallery and select media
        Button uploadButton = findViewById(R.id.button_upload);
        uploadButton.setOnClickListener(v -> openGallery());

        // Button to view saved media
        Button viewButton = findViewById(R.id.button_trans);
        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedMediaActivity.class);
            startActivity(intent);
            transmittedFilesUnreadCount = 0;
            updateUnreadCounts();
            // Save the updated count to SharedPreferences
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt(TRANSMITTED_COUNT_KEY, transmittedFilesUnreadCount);
            editor.apply();
        });

        // Button to take a photo or video
        Button takeButton = findViewById(R.id.button_take);
        takeButton.setOnClickListener(v -> openCamera());

        // Button to open Watch Directory
        Button watchDirectoryButton = findViewById(R.id.button_watch_directory);
        watchDirectoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WatchDirectoryActivity.class);
            startActivity(intent);
            watchDirectoryUnreadCount = 0;
            updateUnreadCounts();
        });

        // Initialize unread counts
        updateUnreadCounts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister BroadcastReceiver
        unregisterReceiver(transmittedCountReceiver);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/* video/*");
        startActivityForResult(intent, PICK_IMAGE_VIDEO_REQUEST);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Media");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        capturedMediaUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedMediaUri);
        startActivityForResult(intent, CAPTURE_IMAGE_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri != null) {
                saveMediaToWatchDirectory(selectedMediaUri);
                generateAndDisplayHash(selectedMediaUri);
                watchDirectoryUnreadCount++;
                updateUnreadCounts();
            }
        } else if (requestCode == CAPTURE_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            if (capturedMediaUri != null) {
                saveMediaToWatchDirectory(capturedMediaUri);
                generateAndDisplayHash(capturedMediaUri);
                watchDirectoryUnreadCount++;
                updateUnreadCounts();
            }
        }
    }

    private void saveMediaToWatchDirectory(Uri mediaUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(mediaUri);
            File appDir = new File(getFilesDir(), "watch_directory");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            File mediaFile = new File(appDir, System.currentTimeMillis() + "." + getFileExtension(mediaUri));
            OutputStream outputStream = new FileOutputStream(mediaFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            // Save upload time to SharedPreferences
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString(UPLOAD_TIME_KEY_PREFIX + mediaFile.getName(), currentTime);
            editor.apply();

            // Send broadcast to update the list in WatchDirectoryActivity
            Intent broadcastIntent = new Intent(WatchDirectoryActivity.ACTION_UPDATE_WATCH_DIRECTORY);
            broadcastIntent.putExtra("newFile", mediaFile.getAbsolutePath());
            sendBroadcast(broadcastIntent);

            Toast.makeText(this, "Media saved successfully to Watch Directory", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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

    private void generateAndDisplayHash(Uri mediaUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(mediaUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            byte[] mediaBytes = byteArrayOutputStream.toByteArray();

            byte[] hash = Sha256.hash(mediaBytes);
            String hashString = bytesToHex(hash);

            Log.d("SHA-256 Hash", hashString);
            Toast.makeText(this, "SHA-256 Hash: " + hashString, Toast.LENGTH_LONG).show();

            inputStream.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate hash", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void updateUnreadCounts() {
        unreadWatchDirectoryCount.setText(String.valueOf(watchDirectoryUnreadCount));
        unreadTransmittedFilesCount.setText(String.valueOf(transmittedFilesUnreadCount));
    }
}
