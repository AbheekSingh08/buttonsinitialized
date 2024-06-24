package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WatchDirectoryActivity extends AppCompatActivity {
    private static final String TAG = "WatchDirectoryActivity";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String NEW_FILES_COUNT_KEY = "new_files_count";
    private static final String TRANSMITTED_COUNT_KEY = "transmitted_count";
    private static final String UPLOAD_TIME_KEY_PREFIX = "upload_time_";
    private static final int PICK_IMAGE_VIDEO_REQUEST = 1;
    private static final int PICK_ZIP_FILE_REQUEST = 3;
    private static final int PICK_ANY_FILE_REQUEST = 4;
    private static final int CAPTURE_IMAGE_VIDEO_REQUEST = 2;

    private ListView listView;
    private ArrayList<File> mediaFiles;
    private MediaAdapter adapter;
    private SharedPreferences sharedPreferences;
    private TextView totalFilesText;
    private int newFilesCount;
    private Uri capturedMediaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_directory);

        // Request write permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        totalFilesText = findViewById(R.id.total_files_text);

        listView = findViewById(R.id.listView);
        mediaFiles = getWatchDirectoryFiles();
        adapter = new MediaAdapter(this, mediaFiles);
        listView.setAdapter(adapter);

        Button uploadZipButton = findViewById(R.id.button_upload_zip);
        uploadZipButton.setOnClickListener(v -> openZipFilePicker());

        Button uploadPhotosButton = findViewById(R.id.button_upload_photos);
        uploadPhotosButton.setOnClickListener(v -> openGallery());

        Button takePhotoVideoButton = findViewById(R.id.button_take_photo_video);
        takePhotoVideoButton.setOnClickListener(v -> openCamera());

        Button uploadOtherFilesButton = findViewById(R.id.button_other_files);
        uploadOtherFilesButton.setOnClickListener(v -> openFilePicker());

        Button viewActivityButton = findViewById(R.id.button_view_activity);
        viewActivityButton.setOnClickListener(v -> {
            Intent intent = new Intent(WatchDirectoryActivity.this, WatchActivityLogActivity.class);
            startActivity(intent);
        });

        // Initialize total files count
        updateTotalFilesCount();

        // Initialize new files count
        newFilesCount = sharedPreferences.getInt(NEW_FILES_COUNT_KEY, 0);
        showNewFilesToast();
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

    private void openZipFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        startActivityForResult(intent, PICK_ZIP_FILE_REQUEST);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_ANY_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri != null) {
                saveFileToWatchDirectory(selectedMediaUri);
            }
        } else if (requestCode == CAPTURE_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            if (capturedMediaUri != null) {
                saveFileToWatchDirectory(capturedMediaUri);
            }
        } else if (requestCode == PICK_ZIP_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedZipUri = data.getData();
            if (selectedZipUri != null) {
                saveFileToWatchDirectory(selectedZipUri);
            }
        } else if (requestCode == PICK_ANY_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                saveFileToWatchDirectory(selectedFileUri);
            }
        }
    }

    private void saveFileToWatchDirectory(Uri fileUri) {
        ContentResolver contentResolver = getContentResolver();
        String fileName = getFileName(fileUri);
        if (fileName == null) {
            Toast.makeText(this, "Failed to get file name", Toast.LENGTH_SHORT).show();
            return;
        }

        File appDir = new File(getFilesDir(), "watch_directory");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        File file = new File(appDir, fileName);

        try (InputStream inputStream = getInputStreamForVirtualFile(contentResolver, fileUri);
             OutputStream outputStream = new FileOutputStream(file);
             BufferedInputStream bis = new BufferedInputStream(inputStream);
             BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }

            // Save upload time to SharedPreferences
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString(UPLOAD_TIME_KEY_PREFIX + file.getName(), currentTime);
            editor.apply();

            // Add new file to the list and update UI
            mediaFiles.add(0, file);
            adapter.notifyDataSetChanged();
            updateTotalFilesCount();
            incrementNewFilesCount();
            showNewFilesToast();

            // Log the activity
            String fileType = contentResolver.getType(fileUri);
            long fileSize = file.length();
            String logMessage = "Added: " + fileName + " | Type: " + fileType + " | Size: " + fileSize + " bytes | Time: " + currentTime;
            logActivity(logMessage);

            Toast.makeText(this, "File saved successfully to Watch Directory", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
            Toast.makeText(this, "Failed to save file: File not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save file", e);
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }

    private InputStream getInputStreamForVirtualFile(ContentResolver contentResolver, Uri uri) throws IOException {
        if (DocumentsContract.isDocumentUri(this, uri)) {
            if (isVirtualFile(contentResolver, uri)) {
                String mimeType = contentResolver.getType(uri);
                String[] openableMimeTypes = contentResolver.getStreamTypes(uri, mimeType);
                if (openableMimeTypes != null && openableMimeTypes.length > 0) {
                    return contentResolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null).createInputStream();
                }
            } else {
                return contentResolver.openInputStream(uri);
            }
        } else {
            return contentResolver.openInputStream(uri);
        }
        throw new FileNotFoundException("File is virtual and not openable: " + uri);
    }

    private boolean isVirtualFile(ContentResolver contentResolver, Uri uri) {
        if (!DocumentsContract.isDocumentUri(this, uri)) {
            return false;
        }

        Cursor cursor = contentResolver.query(uri, new String[]{DocumentsContract.Document.COLUMN_FLAGS}, null, null, null);
        int flags = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    flags = cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return (flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
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

    private void updateTotalFilesCount() {
        int totalFiles = mediaFiles.size();
        totalFilesText.setText("Total Files: " + totalFiles);
    }

    private void incrementNewFilesCount() {
        newFilesCount++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(NEW_FILES_COUNT_KEY, newFilesCount);
        editor.apply();
    }

    private void showNewFilesToast() {
        if (newFilesCount > 0) {
            //Toast.makeText(this, "There are " + newFilesCount + " new files", Toast.LENGTH_LONG).show();
        }
    }

    private void logActivity(String message) {
        SharedPreferences prefs = getSharedPreferences("ActivityLogs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int logCount = prefs.getInt("log_count", 0);
        // Add the new log entry at the beginning
        for (int i = logCount; i > 0; i--) {
            String previousLog = prefs.getString("log_" + (i - 1), "");
            editor.putString("log_" + i, previousLog);
        }
        editor.putString("log_0", message);
        editor.putInt("log_count", logCount + 1);
        editor.apply();
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
            String fileType = getFileType(file);

            if (fileType.equals("application/zip")) {
                thumbnail.setImageResource(R.drawable.ic_folder); // Use your folder icon resource
            } else if (fileType.startsWith("image") || fileType.startsWith("video")) {
                thumbnail.setImageURI(Uri.fromFile(file));
            } else if (fileType.equals("application/pdf")) {
                thumbnail.setImageResource(R.drawable.ic_other); // Use your PDF icon resource
            } else {
                thumbnail.setImageResource(R.drawable.ic_other); // Use your "Other" icon resource
            }

            String uploadTimeString = sharedPreferences.getString(UPLOAD_TIME_KEY_PREFIX + file.getName(), "Unknown");
            uploadTime.setText(uploadTimeString);

            if (file.getName().endsWith(".sha.txt")) {
                thumbnail.setVisibility(View.GONE);
                viewButton.setText("View txt");
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
                    if (fileType.equals("application/pdf")) {
                        Intent intent = new Intent(context, PdfViewActivity.class);
                        Uri fileUri = FileProvider.getUriForFile(context, "com.example.myapplication.fileprovider", file);
                        intent.putExtra("pdfUri", fileUri.toString());
                        context.startActivity(intent);
                    } else if (fileType.equals("application/zip")) {
                        Intent intent = new Intent(context, ZipViewActivity.class);
                        intent.putExtra("zipFilePath", file.getAbsolutePath());
                        context.startActivity(intent);
                    } else {
                        Intent intent = new Intent(context, FullScreenMediaActivity.class);
                        Uri fileUri = FileProvider.getUriForFile(context, "com.example.myapplication.fileprovider", file);
                        intent.putExtra("mediaUrl", fileUri.toString());
                        if (fileType.startsWith("image")) {
                            intent.putExtra("mediaType", "image");
                        } else if (fileType.startsWith("video")) {
                            intent.putExtra("mediaType", "video");
                        }
                        context.startActivity(intent);
                    }
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
                                updateTotalFilesCount();

                                // Log the activity
                                String logMessage = "Deleted: " + file.getName() + " | Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                logActivity(logMessage);
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

                    // Log the activity
                    String logMessage = "Transmitted: " + file.getName() + " | Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    logActivity(logMessage);

                    // Optionally update some UI or send another broadcast if needed
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
