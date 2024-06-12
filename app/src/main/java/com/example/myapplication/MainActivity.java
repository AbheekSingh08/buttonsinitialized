package com.example.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_VIDEO_REQUEST = 1;
    private static final int CAPTURE_IMAGE_VIDEO_REQUEST = 2;
    private Uri capturedMediaUri;

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

        // Button to open the gallery and select media
        Button uploadButton = findViewById(R.id.button_upload);
        uploadButton.setOnClickListener(v -> openGallery());

        // Button to view saved media
        Button viewButton = findViewById(R.id.button_view);
        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PasscodeActivity.class);
            startActivity(intent);
        });

        // Button to take a photo or video
        Button takeButton = findViewById(R.id.button_take);
        takeButton.setOnClickListener(v -> openCamera());
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
                saveMediaToAppDirectory(selectedMediaUri);
                generateAndDisplayHash(selectedMediaUri);
            }
        } else if (requestCode == CAPTURE_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            if (capturedMediaUri != null) {
                saveMediaToAppDirectory(capturedMediaUri);
                generateAndDisplayHash(capturedMediaUri);
            }
        }
    }

    private void saveMediaToAppDirectory(Uri mediaUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(mediaUri);
            File appDir = new File(getFilesDir(), "saved_media");
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

            Toast.makeText(this, "Media saved successfully", Toast.LENGTH_SHORT).show();
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

            //Log.d("SHA-256 Hash", hashString);
            //Toast.makeText(this, "SHA-256 Hash: " + hashString, Toast.LENGTH_LONG).show();

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
}
