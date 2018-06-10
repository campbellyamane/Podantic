package com.campbellyamane.podantic;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.campbellyamane.podantic.General.screenWidth;

public class PodcastAdapter extends ArrayAdapter<Podcast> {

    private Context mContext;
    private List<Podcast> podcastList;

    public PodcastAdapter(Activity context, ArrayList<Podcast> list) {
        super(context, 0 , list);
        podcastList = list;
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(
                    R.layout.podcast_item, parent, false);
        }

        Podcast currentPod = podcastList.get(position);

        ImageView image = (ImageView) listItem.findViewById(R.id.podcast);
        TextView name = (TextView) listItem.findViewById(R.id.pod_name);
        Picasso.get().load(currentPod.getArt()).fit().centerCrop().into(image);
        name.setText(currentPod.getName());
        return listItem;
    }

    @Override
    public int getCount(){
        return podcastList.size();
    }

    public void update(ArrayList<Podcast> upd){
        podcastList = upd;
        this.notifyDataSetChanged();

    }
}
