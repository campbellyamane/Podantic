package com.campbellyamane.podantic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class PodcastService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer pp;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    private AudioManager audioManager;

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (pp == null) initMediaPlayer();
                else if (!pp.isPlaying()) pp.start();
                pp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (pp.isPlaying()) pp.stop();
                pp.release();
                pp = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (pp.isPlaying()) pp.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (pp.isPlaying()) pp.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public class LocalBinder extends Binder {
        public PodcastService getService() {
            return PodcastService.this;
        }
    }

    private void initMediaPlayer() {
        pp = new MediaPlayer();
        //Set up MediaPlayer event listeners
        pp.setOnCompletionListener(this);
        pp.setOnErrorListener(this);
        pp.setOnPreparedListener(this);
        pp.setOnBufferingUpdateListener(this);
        pp.setOnSeekCompleteListener(this);
        pp.setOnInfoListener(this);
        pp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void playMedia(String url){
        try {
            //Reset so that the MediaPlayer is not pointing to another data source
            pp.reset();
            pp.setDataSource(url);
            pp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopMedia() {
        if (pp == null){
            return;
        }
        if (pp.isPlaying()) {
            pp.stop();
        }
    }

    public boolean isPlaying(){
        return pp.isPlaying();
    }

    public void pauseMedia() {
        if (pp.isPlaying()) {
            pp.pause();
            resumePosition = pp.getCurrentPosition();
        }
        else if (!pp.isPlaying()){
            pp.start();
        }
    }

    public void rwMedia(){
        if (pp != null){
            pp.seekTo(pp.getCurrentPosition()-10000);
        }
    }

    public void ffMedia(){
        if (pp != null){
            pp.seekTo(pp.getCurrentPosition()+10000);
        }
    }

    private void resumeMedia() {
        if (!pp.isPlaying()) {
            pp.seekTo(resumePosition);
            pp.start();
        }
    }
    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        stopMedia();
        //stop the service
        stopSelf();
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        pp = mp;
        pp.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pp != null) {
            stopMedia();
            pp.release();
        }
        if (audioManager != null) {
            removeAudioFocus();
        }
    }

}
