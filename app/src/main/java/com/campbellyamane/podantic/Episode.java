package com.campbellyamane.podantic;

import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Episode{

    private String mTitle;
    private String mDate;
    private String mDetails;
    private String mMp3;
    private String mArt;
    private String mTime;
    private String mPod;

    public Episode(String title, String date, String details, String mp3, String art, String time, String pod){
        mTitle = title;
        mDate = date.substring(0,16);
        mDetails = Html.fromHtml(details).toString();
        mMp3 = mp3;
        mArt = art;
        mTime = time;
        mPod = pod;
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
}
