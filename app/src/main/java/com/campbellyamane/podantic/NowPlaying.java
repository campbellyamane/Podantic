package com.campbellyamane.podantic;

import android.content.Context;
import android.content.Intent;
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

import static com.campbellyamane.podantic.MainActivity.serviceBound;
import static com.campbellyamane.podantic.MainActivity.serviceConnection;

public class NowPlaying extends General {

    private ImageView art;
    private TextView title;
    private TextView podcast;
    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        Bundle extras = getIntent().getExtras();
        {
            art = findViewById(R.id.art);
            title = findViewById(R.id.episode);
            podcast = findViewById(R.id.podcast);
            button = findViewById(R.id.play);
        }
        Picasso.get().load(extras.getString("art")).fit().centerCrop().into(art);
        title.setText(extras.getString("episode"));
        podcast.setText(extras.getString("podcast"));

        if (!serviceBound) {
            Intent playerIntent = new Intent(this, PodcastService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            player.newTrack(extras.getString("mp3"));
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }
}
