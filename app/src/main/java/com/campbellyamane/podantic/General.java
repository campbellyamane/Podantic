package com.campbellyamane.podantic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.TreeMap;

public class General extends AppCompatActivity{

    private static DisplayMetrics displayMetrics = new DisplayMetrics();

    public static int screenWidth;

    public static PodcastService player;

    public static Podcast currentPodcast;

    public static StorageUtil storageUtil;

    public static ArrayList<Podcast> subscriptionList;
    public static ArrayList<Podcast> displayList;
    public static TreeMap<String, Integer> categoriesList;
    public static boolean serviceBound = false;

    //Binding this Client to the AudioPlayer Service
    public static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PodcastService.LocalBinder binder = (PodcastService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth =  displayMetrics.widthPixels;

        storageUtil = new StorageUtil(getApplicationContext());
        subscriptionList = storageUtil.loadSubscriptions();
        displayList = (ArrayList<Podcast>) subscriptionList.clone();
        categoriesList = storageUtil.loadCategories();
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, PodcastService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }
}
