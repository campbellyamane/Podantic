package com.campbellyamane.podantic;

import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Episode{

    private String mTitle;
    private String mDate;
    private String mDetails;
    private String mMp3;
    private String mArt;
    private String mTime;
    private String mPod;
    private String mUrl;
    private ArrayList<String> mCats;
    private long mLastPlayed;
    private int mPosition;

    public Episode(String title, String date, String details, String mp3, String art, String time, String pod, String podUrl, ArrayList<String> cats){
        mTitle = title;
        mDate = date.substring(0,16);
        mDetails = Html.fromHtml(details).toString();
        mMp3 = mp3;
        mArt = art;
        if (time.contains(":")) {
            for (int i = 0; i < time.length(); i++) {
                if (time.charAt(i) != '0' && time.charAt(i) != ':') {
                    mTime = time.substring(i);
                    break;
                }
            }
        }
        else{
            mTime = getTime(Integer.parseInt(time));
        }
        mPod = pod;
        mLastPlayed = System.currentTimeMillis();
        mPosition = 0;
        mUrl = podUrl;
        mCats = cats;
    }
    public String getPodcast(){ return mPod; }
    public String getTitle(){
        return mTitle;
    }

    public String getDate(){
        return mDate;
    }

    public String getDetails(){
        return mDetails;
    }

    public String getMp3(){
        return mMp3;
    }

    public String getArt(){
        return mArt;
    }

    public String getTime(){
        return mTime;
    }

    public long getLastPlayed(){
        return mLastPlayed;
    }

    public int getPosition(){
        return mPosition;
    }

    public String getAllEpisodes(){
        return mUrl;
    }

    public ArrayList<String> getCategories(){
        return mCats;
    }

    public void setLastPlayed(long lp){
        mLastPlayed = lp;
    }

    public void setPosition(int p){
        mPosition = p;
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
    public int hashCode() {
        int result = 17;
        result = 31 * result + mTitle.hashCode();
        result = 31 * result + mPosition;
        result = 31 * result + mArt.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o){
        Episode ep = (Episode) o;
        return (mMp3.equals(ep.getMp3()));
    }
}
