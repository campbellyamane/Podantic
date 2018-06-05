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

public class NowPlaying extends AppCompatActivity {

    private ImageView art;
    private TextView title;
    private TextView podcast;
    private Button button;
    private MediaPlayer mp;
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


        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(extras.getString("mp3"));
            mp.prepareAsync();
            // You can show progress dialog here until it prepared to play
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Now dismis progress dialog, Media palyer will start playing
                    mp.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mp.isPlaying()){
                    mp.pause();
                    button.setText(">");
                }
                else if (!mp.isPlaying()){
                    mp.start();
                    button.setText("||");
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mp.stop();
    }
}
