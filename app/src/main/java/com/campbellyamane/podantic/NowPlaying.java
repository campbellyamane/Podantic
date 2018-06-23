package com.campbellyamane.podantic;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

public class NowPlaying extends General implements PodcastService.Callbacks{

    private LinearLayout whole;
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
    private ImageButton more;

    private Boolean createCalled = false;

    private static long sinceDown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in_vert, R.anim.stay);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        player.registerCallbacks(this);
        createCalled = true;
        {
            whole = findViewById(R.id.whole_screen);
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
            more = findViewById(R.id.more);
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

        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(NowPlaying.this, v);
                popup.getMenuInflater().inflate(R.menu.nowplaying_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent;
                        switch (item.getItemId()) {
                            case R.id.go_to_podcast:
                                currentPodcast = new Podcast(currentEpisode.getArt(),currentEpisode.getPodcast(),currentEpisode.getAllEpisodes(),currentEpisode.getCategories());
                                intent = new Intent(getApplicationContext(), PodHome.class);
                                startActivity(intent);
                                break;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

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
                    playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px_white);
                    player.pauseMedia();
                }
                else{
                    playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
                    player.pauseMedia();
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        getSupportActionBar().setTitle("Now Playing");
        //Make sure you update Seekbar on UI thread

        if (!createCalled) {
            if (player.isPlaying()) {
                playButton.setImageResource(R.drawable.ic_baseline_pause_24px_white);
            } else {
                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px_white);
            }
        }
        createCalled = false;

        Picasso.get().load(currentEpisode.getArt()).fit().centerCrop().into(art, new Callback() {
            @Override
            public void onSuccess() {
                themeColor = Palette.from(((BitmapDrawable) art.getDrawable()).getBitmap()).generate().getDominantSwatch().getRgb();
                GradientDrawable gd = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {themeColor,0x000000});
                gd.setCornerRadius(0f);
                whole.setBackground(gd);
            }

            @Override
            public void onError(Exception e) {

            }
        });
        details.setText(currentEpisode.getDetails());
        title.setText(currentEpisode.getTitle());
        podcast.setText(currentEpisode.getPodcast());
        if (isInFavorites(currentEpisode)){
            favorite.setImageResource(R.drawable.ic_baseline_star_24px);
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
        dialog = new ProgressDialog(NowPlaying.this);
        dialog.setTitle("Loading");
        dialog.setMessage("Connecting to " + d[0] + "//" + d[2] + "...");
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onBackPressed();
            }
        });
        dialog.show();
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_from_top);
    }

}
