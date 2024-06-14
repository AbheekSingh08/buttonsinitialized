package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SavedMediaActivity extends AppCompatActivity {

    private ListView listView;
    private List<Map<String, Object>> mediaFiles;
    private MediaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_media);

        listView = findViewById(R.id.listView);
        mediaFiles = new ArrayList<>();

        adapter = new MediaAdapter(this, mediaFiles);
        listView.setAdapter(adapter);

        loadMediaFiles();
    }

    private void loadMediaFiles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("mediaFiles")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            mediaFiles.add(document.getData());
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load media files", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class MediaAdapter extends ArrayAdapter<Map<String, Object>> {

        private Context context;
        private List<Map<String, Object>> files;

        public MediaAdapter(Context context, List<Map<String, Object>> files) {
            super(context, 0, files);
            this.context = context;
            this.files = files;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.media_item, parent, false);
            }

            Map<String, Object> fileData = files.get(position);
            String fileName = (String) fileData.get("fileName");
            String fileUrl = (String) fileData.get("fileUrl");

            TextView fileNameTextView = convertView.findViewById(R.id.file_name);
            ImageView thumbnailImageView = convertView.findViewById(R.id.thumbnail);
            Button viewButton = convertView.findViewById(R.id.button_view);
            Button deleteButton = convertView.findViewById(R.id.button_delete);
            Button hashButton = convertView.findViewById(R.id.button_hash);

            fileNameTextView.setText(fileName);

            // Load thumbnail image using Glide
            Glide.with(context).load(fileUrl).into(thumbnailImageView);

            viewButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenMediaActivity.class);
                intent.putExtra("mediaUrl", fileUrl);

                String fileType = getFileType(fileName);
                if (fileType.startsWith("image")) {
                    intent.putExtra("mediaType", "image");
                } else if (fileType.startsWith("video")) {
                    intent.putExtra("mediaType", "video");
                }

                context.startActivity(intent);
            });

            deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Confirmation")
                        .setMessage("Are you sure you want to delete this file?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        FirebaseFirestore.getInstance().collection("users")
                                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .collection("mediaFiles")
                                                .whereEqualTo("fileUrl", fileUrl)
                                                .get()
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                        for (DocumentSnapshot document : task.getResult()) {
                                                            document.getReference().delete()
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        files.remove(position);
                                                                        notifyDataSetChanged();
                                                                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete metadata", Toast.LENGTH_SHORT).show());
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete metadata", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            });

            hashButton.setOnClickListener(v -> {
                FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                        .getBytes(Long.MAX_VALUE)
                        .addOnSuccessListener(bytes -> {
                            byte[] hash = generateSha256Hash(bytes);
                            String hashString = bytesToHex(hash);

                            new AlertDialog.Builder(context)
                                    .setTitle("SHA-256 Hash")
                                    .setMessage(hashString)
                                    .setPositiveButton("OK", null)
                                    .show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to generate hash", Toast.LENGTH_SHORT).show());
            });

            return convertView;
        }

        private byte[] generateSha256Hash(byte[] data) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return digest.digest(data);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Helper method to determine file type
        private String getFileType(String fileName) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
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
