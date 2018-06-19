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

public class EpisodeAdapter extends ArrayAdapter<Episode>{

    private Context mContext;
    private List<Episode> episodeList;

    public EpisodeAdapter(Activity context, ArrayList<Episode> list) {
        super(context, 0 , list);
        episodeList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(
                    R.layout.episode_item, parent, false);
        }

        Episode currentEpisode = episodeList.get(position);

        ImageView image = (ImageView) listItem.findViewById(R.id.episode_art);
        Picasso.get().load(currentEpisode.getArt()).fit().centerCrop().into(image);

        TextView title = (TextView) listItem.findViewById(R.id.episode_title);
        title.setText(currentEpisode.getTitle());

        TextView details = (TextView) listItem.findViewById(R.id.episode_details);
        details.setText(currentEpisode.getDetails());

        TextView date = (TextView) listItem.findViewById(R.id.episode_date);
        date.setText(currentEpisode.getDate());

        TextView time = (TextView) listItem.findViewById(R.id.episode_time);
        try {
            if (currentEpisode.getMp3().equals(General.currentEpisode.getMp3())) {
                time.setText("");
                time.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_baseline_volume_up_24px_white, 0);
            }
            else {
                time.setText(currentEpisode.getTime());
                time.setCompoundDrawablesWithIntrinsicBounds(0,0, 0, 0);
            }
        }
        catch (Exception e) {
            time.setText(currentEpisode.getTime());
            time.setCompoundDrawablesWithIntrinsicBounds(0,0, 0, 0);
        }
        return listItem;
    }

    @Override
    public int getCount(){
        return episodeList.size();
    }

    public void update(ArrayList<Episode> upd){
        episodeList = upd;
        this.notifyDataSetChanged();

    }
}
