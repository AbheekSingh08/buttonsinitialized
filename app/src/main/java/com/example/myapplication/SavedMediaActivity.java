package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class SavedMediaActivity extends AppCompatActivity {
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
        mediaFiles = getSavedMediaFiles();
        adapter = new MediaAdapter(this, mediaFiles);
        listView.setAdapter(adapter);
    }

    private ArrayList<File> getSavedMediaFiles() {
        ArrayList<File> files = new ArrayList<>();
        File appDir = new File(getFilesDir(), "saved_media");
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
                convertView = LayoutInflater.from(context).inflate(R.layout.media_item, parent, false);
            }
            File file = files.get(position);
            TextView fileName = convertView.findViewById(R.id.file_name);
            ImageView thumbnail = convertView.findViewById(R.id.thumbnail);
            Button viewButton = convertView.findViewById(R.id.button_view);
            Button deleteButton = convertView.findViewById(R.id.button_delete);
            Button hashButton = convertView.findViewById(R.id.button_hash);

            fileName.setText(file.getName());
            thumbnail.setImageURI(Uri.fromFile(file));

            // Handle display and viewing of different file types
            if (file.getName().endsWith(".sha.txt")) {
                // If the file is a hash file, hide the thumbnail
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

            hashButton.setOnClickListener(v -> {
                try {
                    InputStream inputStream = new FileInputStream(file);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        byteArrayOutputStream.write(buffer, 0, length);
                    }
                    byte[] fileBytes = byteArrayOutputStream.toByteArray();

                    byte[] hash = Sha256.hash(fileBytes);
                    String hashString = bytesToHex(hash);

                    new AlertDialog.Builder(context)
                            .setTitle("SHA-256 Hash")
                            .setMessage(hashString)
                            .setPositiveButton("OK", null)
                            .show();

                    // Save hash to a text file in Downloads directory
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File hashFile = new File(downloadsDir, file.getName() + ".sha.txt");
                    FileOutputStream fos = new FileOutputStream(hashFile);
                    fos.write(hashString.getBytes());
                    fos.close();

                    // Add the hash file to the list of media files and update the adapter
                    files.add(hashFile);
                    notifyDataSetChanged();

                    // Notify user of the download
                    Toast.makeText(context, "Hash file saved to Downloads: " + hashFile.getName(), Toast.LENGTH_SHORT).show();

                    inputStream.close();
                } catch (Exception e) {
                    Toast.makeText(context, "Failed to generate hash", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });

            return convertView;
        }

        // Helper method to determine file type
        private String getFileType(File file) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        // Helper method to convert bytes to hex string
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
}
