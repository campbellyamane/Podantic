package com.campbellyamane.podantic;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
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
    private SeekBar seekBar;
    private Handler mHandler;

    private TextView timeElapsed;
    private TextView timeLeft;
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
            seekBar = findViewById(R.id.seekbar);
            timeElapsed = findViewById(R.id.time_elapsed);
            timeLeft = findViewById(R.id.time_left);
            mHandler = new Handler();
        }
        Picasso.get().load(extras.getString("art")).fit().centerCrop().into(art);
        title.setText(extras.getString("episode"));
        podcast.setText(extras.getString("podcast"));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int set;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(player.exists() && fromUser){
                    player.seekTo(progress*1000);
                }
            }
        });
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


        //Make sure you update Seekbar on UI thread
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(player.exists()){
                    int mCurrentPosition = player.getCurrentPosition() / 1000;
                    seekBar.setMax(player.getDuration()/1000);
                    seekBar.setProgress(mCurrentPosition);
                    timeElapsed.setText(getTime(mCurrentPosition));
                    timeLeft.setText("-" + getTime(player.getDuration()/1000 - mCurrentPosition));
                }
                mHandler.postDelayed(this, 250);
            }
        });
    }

    private String getTime(int secs){
        long second = secs % 60;
        long minute = (secs/60) % 60;
        long hour = (secs/3600) % 24;
        if (secs >= 3600) {
            return String.format("%2d:%02d:%02d", hour, minute, second);
        }
        return String.format("%02d:%02d", minute, second);
    }
    @Override
    protected void onPause(){
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }


}
