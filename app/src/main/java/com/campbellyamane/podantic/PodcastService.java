package com.campbellyamane.podantic;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telecom.Call;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static android.app.NotificationManager.IMPORTANCE_MAX;
import static com.campbellyamane.podantic.General.serviceBound;

public class PodcastService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private static MediaPlayer pp;
    private static Episode episode = new Episode("", "49731298537982742984232", "", "", "", "0", "");
    private static Bitmap bitmap;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    public static Boolean isBound = true;

    private AudioManager audioManager;

    private boolean shouldStart = true;

    private int check = 0;

    private static long millis = 0;

    public static final String ACTION_PLAY = "com.campbellyamane.podantic.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.campbellyamane.podantic.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.campbellyamane.podantic.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.campbellyamane.podantic.ACTION_NEXT";
    public static final String ACTION_STOP = "com.campbellyamane.podantic.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSession mediaSession;
    private MediaController.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    private static Notification notification;

    //Callbacks
    private static Callbacks activity;

    private static boolean synced = false;

    //Util
    private static StorageUtil storageUtil;
    private static ArrayList<Episode> inProgressList_service;

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (pp == null) {
                    initMediaPlayer();
                } else if (!pp.isPlaying() && shouldStart) {
                    pauseMedia();
                }
                pp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (pp.isPlaying()) {
                    pauseMedia();
                    shouldStart = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (pp.isPlaying()) {
                    pauseMedia();
                    shouldStart = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (pp.isPlaying()) {
                    pp.setVolume(0.1f, 0.1f);
                }
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
        isBound = true;
        return iBinder;
    }

    public class LocalBinder extends Binder {
        public PodcastService getService() {
            return PodcastService.this;
        }
    }

    private void initMediaPlayer() {
        if (pp == null) {
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
    }

    public Boolean exists() {
        return pp != null;
    }

    public Episode getPlaying() {
        return episode;
    }

    public void playMedia(Episode e) {
        synced = false;
        try{
            if(episode.getMp3().equals(e.getMp3())){
                synced = true;
                if (!pp.isPlaying()){
                    pauseMedia();
                }
                return;
            }
        } catch (Exception ex){
            //nada
        }
        activity.cbPreLoad();
        try {
            if (!episode.getMp3().equals("")) {
                inProgressList_service.get(inProgressList_service.indexOf(episode)).setPosition(pp.getCurrentPosition());
            }
        }catch (Exception ex) {
            episode.setPosition(pp.getCurrentPosition());
            inProgressList_service.add(episode);
        }
        storageUtil.storeInProgress(inProgressList_service);
        inProgressList_service = storageUtil.loadInProgress();

        if (requestAudioFocus()) {
            episode = e;
            try {
                //Reset so that the MediaPlayer is not pointing to another data source
                pp.reset();
                pp.setDataSource(episode.getMp3());
                pp.prepareAsync();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (IllegalStateException ex) {
                try {
                    pp.setDataSource(episode.getMp3());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                pp.prepareAsync();
            }
        }
    }

    public void playDownloadedMedia(Episode e) {
        synced = false;
        try{
            if(episode.getMp3().equals(e.getMp3())){
                synced = true;
                if (!pp.isPlaying()){
                    pauseMedia();
                }
                return;
            }
        } catch (Exception ex){
            //nada
        }

        try {
            if (!episode.getMp3().equals("")) {
                inProgressList_service.get(inProgressList_service.indexOf(episode)).setPosition(pp.getCurrentPosition());
            }
        }catch (Exception ex) {
            episode.setPosition(pp.getCurrentPosition());
            inProgressList_service.add(episode);
        }
        storageUtil.storeInProgress(inProgressList_service);
        inProgressList_service = storageUtil.loadInProgress();

        if (requestAudioFocus()) {
            episode = e;
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PODCASTS);
            String file = new File(path, e.getMp3().hashCode() + ".mp3").getAbsolutePath();
            try {
                //Reset so that the MediaPlayer is not pointing to another data source
                pp.reset();
                pp.setDataSource(file);
                pp.prepare();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (IllegalStateException ex) {
                try {
                    pp.setDataSource(file);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    pp.prepare();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void stopMedia() {
        if (pp == null) {
            return;
        }
        if (pp.isPlaying()) {
            pp.stop();
        }
    }

    public boolean isPlaying() {
        return pp.isPlaying();
    }

    public void pauseMedia() {
        if (pp.isPlaying()) {
            try{
                inProgressList_service.get(inProgressList_service.indexOf(episode)).setPosition(pp.getCurrentPosition());
            } catch (Exception e){
                episode.setPosition(pp.getCurrentPosition());
                inProgressList_service.add(episode);
            }
            storageUtil.storeInProgress(inProgressList_service);
            inProgressList_service = storageUtil.loadInProgress();

            pp.pause();
            removeAudioFocus();
            //resumePosition = pp.getCurrentPosition();
            shouldStart = false;
            activity.cbSetPlay(false);
            buildNotification(PlaybackStatus.PAUSED);
        } else if (!pp.isPlaying() && requestAudioFocus()) {
            pp.start();
            activity.cbSetPlay(true);
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    public void rwMedia() {
        if (pp != null) {
            pp.seekTo(pp.getCurrentPosition() - 10000);
        }
    }

    public void ffMedia() {
        if (pp != null) {
            pp.seekTo(pp.getCurrentPosition() + 10000);
        }
    }

    public int getDuration() {
        if (synced){
            return pp.getDuration();
        }
        return -1;
    }

    public int getCurrentPosition() {
        try {
            return pp.getCurrentPosition();
        } catch (Exception e) {
            return 0;
        }
    }

    public void seekTo(int ms) {
        pp.seekTo(ms);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        activity.cbSetPlay(false);
        try{
            inProgressList_service.remove(episode);
            storageUtil.storeInProgress(inProgressList_service);
            inProgressList_service = storageUtil.loadInProgress();
        } catch (Exception e){
            //nada
        }
        //Invoked when playback of a media source has completed.
        stopMedia();
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
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URL url = new URL(episode.getArt());
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    bitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                activity.cbOnLoad();
                synced = true;
                pp.start();
                buildNotification(PlaybackStatus.PLAYING);
                try {
                    Log.d("PodAntic", Integer.toString(inProgressList_service.size()));
                    pp.seekTo(inProgressList_service.get(inProgressList_service.indexOf(episode)).getPosition());
                } catch (Exception e){
                    //nada
                }
                storageUtil.storeInProgress(inProgressList_service);
                inProgressList_service = storageUtil.loadInProgress();
            }
        });
        pp = mp;
        thread.start();
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
        storageUtil = new StorageUtil(this);
        inProgressList_service = storageUtil.loadInProgress();
        initMediaPlayer();
        try {
            initMediaSession();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, START_NOT_STICKY);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSession(getApplicationContext(), "PodcastPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSession.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                pauseMedia();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
            }

            @Override
            public void onRewind() {
                super.onRewind();
                rwMedia();
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                ffMedia();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    public void buildNotification(PlaybackStatus playbackStatus) {
        Log.d("PodAntic", "built");

        int notificationAction = R.drawable.ic_baseline_pause_24px_notif;//needs to be initialized

        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.ic_baseline_pause_24px_notif;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_baseline_play_arrow_24px_notif;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Intent launch  = new Intent(PodcastService.this, MainActivity.class);
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent launchActivity = PendingIntent.getActivity(this, 0, launch, 0);

        // Create a new Notification
        notification = new Notification.Builder(this)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setShowWhen(false)
                //.setOngoing(true)
                // Set the Notification style
                .setStyle(new Notification.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(1))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.black))
                // Set the large and small icons
                .setLargeIcon(bitmap)
                .setSmallIcon(R.drawable.ic_baseline_rss_feed_24px)
                // Set Notification content information
                .setContentText(episode.getPodcast())
                .setContentTitle(episode.getTitle())
                .setContentInfo(episode.getDate())
                .setContentIntent(launchActivity)
                // Add playback actions
                .addAction(new Notification.Action.Builder(R.drawable.ic_baseline_fast_rewind_24px_notif, "previous", playbackAction(3)).build())
                .addAction(new Notification.Action.Builder(notificationAction, "pause", play_pauseAction).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_baseline_fast_forward_24px_notif, "next", playbackAction(2)).build()).build();

        updateMetaData();
        startForeground(NOTIFICATION_ID, notification);
        if (!isBound && !pp.isPlaying()){
            stopForeground(false);
        }
    }

    public void removeNotification() {
        stopForeground(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, PodcastService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            stopForeground(false);
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.fastForward();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.rewind();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private void updateMetaData() {
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, episode.getPodcast())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, episode.getDate())
                .putString(MediaMetadata.METADATA_KEY_TITLE, episode.getTitle())
                .build());

        if (!episode.getMp3().equals("")) {
            PlaybackState.Builder state = new PlaybackState.Builder();
            if (pp.isPlaying()) {
                state.setState(PlaybackState.STATE_PLAYING, pp.getCurrentPosition(), 1.0f);
            } else {
                state.setState(PlaybackState.STATE_PAUSED, pp.getCurrentPosition(), 1.0f);
            }
            mediaSession.setPlaybackState(state.build());
        }
    }

    public void registerCallbacks(Activity activity) {
        this.activity = (Callbacks) activity;
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks {
        public void cbSetPlay(boolean play);

        public void cbOnLoad();

        public void cbPreLoad();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        if (!pp.isPlaying()) {
            stopSelf();
        }
    }

}
