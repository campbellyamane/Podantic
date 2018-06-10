package com.campbellyamane.podantic;

import java.util.ArrayList;

public class Podcast {

    private String mArt;
    private String mName;
    private String mFeed;
    private ArrayList<String> mCats;

    public Podcast(String art, String name, String feed, ArrayList<String> categories){
        mArt = art;
        mName = name;
        mFeed = feed;
        mCats = categories;
    }

    public String getArt(){
        return mArt;
    }

    public String getName(){
        return mName;
    }

    public String getFeed(){
        return mFeed;
    }

    public ArrayList<String> getCategories(){
        return mCats;
    }

    public void setArt(String art){
        mArt = art;
    }
}
