package com.campbellyamane.podantic;

public class Podcast {

    private String mArt;
    private String mName;
    private String mFeed;

    public Podcast(String art, String name, String feed){
        mArt = art;
        mName = name;
        mFeed = feed;
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
}
