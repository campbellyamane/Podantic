package com.campbellyamane.podantic;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class NowPlaying extends General {

    private ImageView art;
    private TextView title;
    private TextView podcast;
    private Button playButton;
    private Button rwButton;
    private Button ffButton;
    private MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        final Bundle extras = getIntent().getExtras();
        {
            art = findViewById(R.id.art);
            title = findViewById(R.id.episode);
            podcast = findViewById(R.id.podcast);
            playButton = findViewById(R.id.play);
            rwButton = findViewById(R.id.rw);
            ffButton = findViewById(R.id.ff);
        }
        Picasso.get().load(extras.getString("art")).fit().centerCrop().into(art);
        title.setText(extras.getString("episode"));
        podcast.setText(extras.getString("podcast"));
        player.playMedia(extras.getString("mp3"));

        rwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.rwMedia();
            }
        });

        ffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.ffMedia();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()){
                    player.pauseMedia();
                    playButton.setText(">");
                }
                else{
                    player.pauseMedia();
                    playButton.setText("| |");
                }
            }
        });
    }
}
