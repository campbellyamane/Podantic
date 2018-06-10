package com.campbellyamane.podantic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

public class StorageUtil {
    private final String STORAGE = "com.campbellyamane.podantic.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    public void storeSubscriptions(ArrayList<Podcast> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Collections.sort(arrayList, new Comparator<Podcast>() {
            @Override
            public int compare(Podcast p1, Podcast p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("subscriptionList", json);
        editor.apply();
    }

    public ArrayList<Podcast> loadSubscriptions() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("subscriptionList", null);
        Type type = new TypeToken<ArrayList<Podcast>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new ArrayList<Podcast>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    public void storeCategories(TreeMap<String, Integer> cats) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(cats);
        editor.putString("categoriesList", json);
        editor.apply();
    }

    public TreeMap<String, Integer> loadCategories() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("categoriesList", null);
        Type type = new TypeToken<TreeMap<String, Integer>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new TreeMap<String, Integer>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    /*

    public void storeAudioIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public int loadAudioIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", -1);//return -1 if no data found
    }

    public void clearCachedAudioPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }*/
}
