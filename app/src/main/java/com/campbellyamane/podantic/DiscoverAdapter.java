package com.campbellyamane.podantic;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.campbellyamane.podantic.General.player;

public class DiscoverAdapter extends ArrayAdapter<Podcast>{

    private Context mContext;
    private List<Podcast> podcastList;

    public DiscoverAdapter(Activity context, ArrayList<Podcast> list) {
        super(context, 0 , list);
        podcastList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(
                    R.layout.top_podcast_item, parent, false);
        }

        Podcast currentPodcast = podcastList.get(position);

        ImageView image = (ImageView) listItem.findViewById(R.id.podcast_art);
        Picasso.get().load(currentPodcast.getArt()).fit().centerCrop().into(image);

        TextView title = (TextView) listItem.findViewById(R.id.podcast_title);
        title.setText(Integer.toString(position + 1)+". " + currentPodcast.getName());

        TextView details = (TextView) listItem.findViewById(R.id.podcast_description);
        details.setText(currentPodcast.getArtist());
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
