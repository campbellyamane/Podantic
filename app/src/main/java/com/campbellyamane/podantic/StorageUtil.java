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
import java.util.LinkedHashMap;
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
                return p1.getName().replace("The ", "").compareTo(p2.getName().replace("The ", ""));
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

    public void storeFavorites(ArrayList<Episode> arrayList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("favoritesList", json);
        editor.apply();
    }

    public ArrayList<Episode> loadFavorites() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("favoritesList", null);
        Type type = new TypeToken<ArrayList<Episode>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new ArrayList<Episode>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    public void storeInProgress(ArrayList<Episode> arrayList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("inProgressList", json);
        editor.apply();
    }

    public ArrayList<Episode> loadInProgress() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("inProgressList", null);
        Type type = new TypeToken<ArrayList<Episode>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new ArrayList<Episode>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    public void storeDownloads(ArrayList<Episode> arrayList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("downloadsList", json);
        editor.apply();
    }

    public ArrayList<Episode> loadDownloads() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("downloadsList", null);
        Type type = new TypeToken<ArrayList<Episode>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new ArrayList<Episode>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    public void storeLastPlayed(ArrayList<Episode> arrayList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("lastPlayedList", json);
        editor.apply();
    }

    public ArrayList<Episode> loadLastPlayed() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("lastPlayedList", null);
        Type type = new TypeToken<ArrayList<Episode>>() {
        }.getType();
        if (gson.fromJson(json,type) == null){
            return new ArrayList<Episode>();
        }
        else{
            return gson.fromJson(json, type);
        }
    }

    public LinkedHashMap<String, String> loadDiscover(){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("All Categories", "26");
        map.put("Arts", "1301");
        map.put("Business", "1321");
        map.put("Comedy", "1303");
        map.put("Education", "26");
        map.put("Games & Hobbies", "1323");
        map.put("Government & Organizations", "1325");
        map.put("Health", "1307");
        map.put("Kids & Family", "1305");
        map.put("Music", "1310");
        map.put("News & Politics", "1311");
        map.put("Religion & Spirituality", "1314");
        map.put("Science & Medicine", "1315");
        map.put("Society & Culture", "1324");
        map.put("Sports & Recreation", "1316");
        map.put("Technology", "1318");
        map.put("TV & Film", "1309");
        return map;
    }
}
