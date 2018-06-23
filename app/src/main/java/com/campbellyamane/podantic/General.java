package com.campbellyamane.podantic;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.TreeMap;

public class General extends AppCompatActivity{

    private static DisplayMetrics displayMetrics = new DisplayMetrics();

    public static int screenWidth;

    public static PodcastService player;

    public static Podcast currentPodcast;
    public static Episode currentEpisode;

    public static StorageUtil storageUtil;

    public static ArrayList<Podcast> subscriptionList;
    public static ArrayList<Podcast> displayList;
    public static ArrayList<Episode> favoritesList;
    public static ArrayList<Episode> downloadsList;
    public static ArrayList<Episode> lastPlayedList;
    public static TreeMap<String, Integer> categoriesList;
    public static boolean serviceBound = false;

    public static Toolbar myToolbar;
    public static DrawerLayout myDrawer;
    public static NavigationView navigationView;

    public static LinearLayout nowPlayingLayout;
    public static ImageView nowPlayingImage;
    public static TextView nowPlayingTitle;
    public static TextView nowPlayingPodcast;
    public static ImageButton nowPlayingButton;

    public static TextView lpPlay;
    public static TextView lpFavorite;
    public static TextView lpDownload;

    public static int themeColor = 0;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth =  displayMetrics.widthPixels;

        storageUtil = new StorageUtil(getApplicationContext());
        subscriptionList = storageUtil.loadSubscriptions();
        displayList = (ArrayList<Podcast>) subscriptionList.clone();
        favoritesList = storageUtil.loadFavorites();
        categoriesList = storageUtil.loadCategories();
        downloadsList = storageUtil.loadDownloads();
        lastPlayedList = storageUtil.loadLastPlayed();
    }

    @Override
    protected void onResume(){
        super.onResume();
        PodcastService.isBound = true;
        setupNav();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                myDrawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupNav(){
        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24px);

        myDrawer = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        myDrawer.closeDrawers();
                        Intent intent;
                        switch (menuItem.getItemId()) {
                            case R.id.subscriptions:
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                                if (!getSupportActionBar().getTitle().equals("My Subscriptions")){
                                    startActivity(intent);
                                }
                                break;

                            case R.id.favorites:
                                intent = new Intent(getApplicationContext(), Favorites.class);
                                if (!getSupportActionBar().getTitle().equals("My Favorites")){
                                    startActivity(intent);
                                }
                                break;

                            case R.id.downloads:
                                intent = new Intent(getApplicationContext(), Downloads.class);
                                if (!getSupportActionBar().getTitle().equals("My Downloads")){
                                    startActivity(intent);
                                }
                                break;

                            case R.id.discover:
                                intent = new Intent(getApplicationContext(), Discover.class);
                                if (!getSupportActionBar().getTitle().equals("Discover")){
                                    startActivity(intent);
                                }
                                break;

                            case R.id.recently_played:
                                intent = new Intent(getApplicationContext(), LastPlayed.class);
                                if (!getSupportActionBar().getTitle().equals("Recently Played")){
                                    startActivity(intent);
                                }
                                break;

                            case R.id.in_progress:
                                intent = new Intent(getApplicationContext(), InProgress.class);
                                if (!getSupportActionBar().getTitle().equals("Playback in Progress")){
                                    startActivity(intent);
                                }
                                break;
                        }
                        return true;
                    }
                });

    }

    public void nowPlayingView(){
        nowPlayingLayout = findViewById(R.id.nowplaying);
        nowPlayingImage = findViewById(R.id.nowplaying_image);
        nowPlayingTitle = findViewById(R.id.nowplaying_title);
        nowPlayingPodcast = findViewById(R.id.nowplaying_podcast);
        nowPlayingButton = findViewById(R.id.nowplaying_button);

        nowPlayingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(General.this, NowPlaying.class);
                i.putExtra("fromNowPlaying", "ya");
                currentEpisode = player.getPlaying();
                startActivity(i);
            }
        });

        try {
            if (player.exists() && player.getPlaying().getMp3() != "") {
                nowPlayingTitle.setSelected(true);
                nowPlayingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (player.isPlaying()){
                            player.pauseMedia();
                            nowPlayingButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px);
                        }
                        else{
                            player.pauseMedia();
                            nowPlayingButton.setImageResource(R.drawable.ic_baseline_pause_24px);
                        }
                    }
                });
                nowPlayingLayout.setVisibility(View.VISIBLE);
                Episode ep = player.getPlaying();
                if (player.isPlaying()){
                    nowPlayingButton.setImageResource(R.drawable.ic_baseline_pause_24px);
                }
                else {
                    nowPlayingButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px);
                }
                Picasso.get().load(ep.getArt()).fit().centerCrop().into(nowPlayingImage);
                nowPlayingTitle.setText(ep.getTitle());
                nowPlayingPodcast.setText(ep.getPodcast());
            }
            else{
                nowPlayingLayout.setVisibility(View.GONE);
            }
        } catch (Exception e){
            nowPlayingLayout.setVisibility(View.GONE);
        }
    }

    public boolean updateFavorites(Episode e){
        if (favoritesList.contains(e)) {
            favoritesList.remove(e);
            storageUtil.storeFavorites(favoritesList);
            favoritesList = storageUtil.loadFavorites();
            return false;
        }
        favoritesList.add(e);
        storageUtil.storeFavorites(favoritesList);
        favoritesList = storageUtil.loadFavorites();
        return true;
    }

    public boolean isInFavorites(Episode e){
        if (favoritesList.contains(e)){
            return true;
        }
        return false;
    }

    public boolean isInDownloads(Episode e){
        if (downloadsList.contains(e)){
            return true;
        }
        return false;
    }

    public void setLpVariables(Dialog menu){
        lpPlay = (TextView) menu.findViewById(R.id.play);
        lpFavorite = (TextView) menu.findViewById(R.id.favorites);
        lpDownload = (TextView) menu.findViewById(R.id.download);
    }

    public void downloadEpisode(Episode ep){
        downloadsList.add(ep);
        storageUtil.storeDownloads(downloadsList);
        Uri url = Uri.parse(ep.getMp3());
        DownloadManager.Request r = new DownloadManager.Request(url);
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, ep.getMp3().hashCode() + ".mp3");
        r.setTitle(ep.getTitle());
        // Notify user when download is completed
        // (Seems to be available since Honeycomb only)
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Start download
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(r);
        /*
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };
        registerReceiver(receiver, filter);
        */
    }

    public void deleteDownload(Episode ep){
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PODCASTS);
        File file = new File(path, ep.getMp3().hashCode() + ".mp3");
        Log.d("PodAntic", file.getAbsolutePath());
        Log.d("PodAntic", Boolean.toString(file.exists()));
        file.delete();
        downloadsList.remove(ep);
        storageUtil.storeDownloads(downloadsList);
    }


    public void checkCategories(Boolean newSub, Podcast cp){
        ArrayList<String> currentCats = cp.getCategories();
        if (newSub){
            try {
                categoriesList.put("All Categories", categoriesList.get("All Categories") + 1);
            } catch (Exception e){
                categoriesList.put("All Categories", 1);
            }
            for (int i = 0; i < currentCats.size(); i++){
                String key = currentCats.get(i);
                if (categoriesList.containsKey(key)){
                    categoriesList.put(key, categoriesList.get(key) + 1);
                }
                else{
                    categoriesList.put(key, 1);
                }
            }
        }
        else{
            for (int i = 0; i < currentCats.size(); i++) {
                String key = currentCats.get(i);
                categoriesList.put(key, categoriesList.get(key) - 1);
            }
            categoriesList.put("All Categories", categoriesList.get("All Categories") - 1);
        }
        storageUtil.storeCategories(categoriesList);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_from_left);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PodcastService.isBound = false;
    }
}
