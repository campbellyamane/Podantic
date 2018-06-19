package com.campbellyamane.podantic;

import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class NowPlaying extends General implements PodcastService.Callbacks{

    private RelativeLayout main;
    private ImageView art;
    private TextView details;
    private TextView title;
    private TextView podcast;
    private ImageButton playButton;
    private ImageButton rwButton;
    private ImageButton ffButton;
    private SeekBar seekBar;
    private Handler mHandler;

    private TextView timeElapsed;
    private TextView timeLeft;

    private ProgressDialog dialog;

    private ImageButton favorite;

    private static long sinceDown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in_vert, R.anim.stay);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        player.registerCallbacks(this);

        {
            main = findViewById(R.id.main_info);
            art = findViewById(R.id.art);
            details = findViewById(R.id.details);
            title = findViewById(R.id.episode);
            title.setSelected(true);
            podcast = findViewById(R.id.podcast);
            playButton = findViewById(R.id.play);
            rwButton = findViewById(R.id.rw);
            ffButton = findViewById(R.id.ff);
            seekBar = findViewById(R.id.seekbar);
            timeElapsed = findViewById(R.id.time_elapsed);
            timeLeft = findViewById(R.id.time_left);
            mHandler = new Handler();
            favorite = findViewById(R.id.favorite);
        }

        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (updateFavorites(currentEpisode)){
                    favorite.setImageResource(R.drawable.ic_baseline_star_24px);
                    Toast.makeText(NowPlaying.this, "Episode Added to Favorites", Toast.LENGTH_SHORT).show();
                }
                else{
                    favorite.setImageResource(R.drawable.ic_baseline_star_border_24px);
                    Toast.makeText(NowPlaying.this, "Episode Removed from Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });


        Picasso.get().load(currentEpisode.getArt()).fit().centerCrop().into(art);
        details.setText(currentEpisode.getDetails());
        title.setText(currentEpisode.getTitle());
        podcast.setText(currentEpisode.getPodcast());

        main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (art.getAlpha() == 1f){
                    art.setAlpha(1f);
                    details.setAlpha(0f);
                    art.animate().alpha(0.3f).setDuration(1000).start();
                    details.animate().alpha(1f).setDuration(1000).start();
                    details.setMovementMethod(new ScrollingMovementMethod());
                }

            }
        });

        details.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && System.currentTimeMillis() - sinceDown < 90) {
                    art.setAlpha(0.3f);
                    details.setAlpha(1f);
                    art.animate().alpha(1f).setDuration(1000).start();
                    details.animate().alpha(0f).setDuration(1000).start();
                    details.setMovementMethod(null);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sinceDown = System.currentTimeMillis();
                }

                return false;
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                    playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px_white);
                }
                else{
                    player.pauseMedia();
                    playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        getSupportActionBar().setTitle("Now Playing");
        //Make sure you update Seekbar on UI thread

        if (isInFavorites(currentEpisode)){
            favorite.setImageResource(R.drawable.ic_baseline_star_24px);
        }

        try{
            Bundle b = getIntent().getExtras();
            String check = b.getString("fromNowPlaying");
            if (player.isPlaying()){
                playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
            }
            else{
                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px_white);
            }
        } catch (Exception e) {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
            if (downloadsList.contains(currentEpisode)) {
                player.playDownloadedMedia(currentEpisode);
            } else {
                player.playMedia(currentEpisode);
            }
        }

        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                int mCurrentPosition = player.getCurrentPosition() / 1000;
                seekBar.setMax(player.getDuration()/1000);
                seekBar.setProgress(mCurrentPosition);
                timeElapsed.setText(getTime(mCurrentPosition));
                timeLeft.setText("-" + getTime(player.getDuration()/1000 - mCurrentPosition));
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

    @Override
    public void cbSetPlay(boolean play){
        Log.d("PodAntic","cb");
        if (play){
            playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
        }
        else {
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px_white);
        }
    }

    @Override
    public void cbOnLoad(){
        try {
            dialog.cancel();
        } catch (Exception e){
            //nada
        }
    }

    @Override
    public void cbPreLoad(){
        String[] d = currentEpisode.getMp3().split("/");
        dialog = ProgressDialog.show(NowPlaying.this, "Loading",
                "Connecting to " + d[0] + "//" + d[2] + "...", true);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_from_top);
    }


}
