package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class FullScreenMediaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_media);

        ImageView imageView = findViewById(R.id.full_screen_image);
        VideoView videoView = findViewById(R.id.full_screen_video);

        Uri mediaUri = getIntent().getParcelableExtra("mediaUri");
        String mediaType = getIntent().getStringExtra("mediaType");

        if ("image".equals(mediaType)) {
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            imageView.setImageURI(mediaUri);
        } else if ("video".equals(mediaType)) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(mediaUri);
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            videoView.start();
        }
    }
}
