package com.campbellyamane.podantic;

import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


//activity for downloads, uses service callbacks to update icons according to playback
public class Downloads extends General implements PodcastService.Callbacks{

    private ListView listView;
    private EditText epSearch;
    private EpisodeAdapter adapter;
    private ArrayList<Episode> searched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_from_right);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        {
            listView = (ListView) findViewById(R.id.episodes);
        }

        //loading episodes into listview
        adapter = new EpisodeAdapter(this, downloadsList);

        listView.setAdapter(adapter);

        //clicking on episode in list opens track in nowplaying activity, resumes if possible
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Downloads.this, NowPlaying.class);
                if (searched != null && searched.size() != downloadsList.size()) {
                    currentEpisode = searched.get(position);
                }
                else{
                    currentEpisode = downloadsList.get(position);
                }
                startActivity(intent);
            }
        });

        //longpress episode opens options menu
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (searched != null && searched.size() != downloadsList.size()) {
                    showMenu(searched.get(position));
                }
                else{
                    showMenu(downloadsList.get(position));
                }
                return true;
            }
        });

        epSearch = findViewById(R.id.episode_search);

        //searching episodes by description or title
        epSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String se = s.toString().toLowerCase();
                if (s.toString().length() > 0) {
                    searched = new ArrayList<Episode>();
                    for (int i = 0; i < downloadsList.size(); i++) {
                        if (downloadsList.get(i).getTitle().toLowerCase().contains(se) || downloadsList.get(i).getDetails().toLowerCase().contains(se)) {
                            searched.add(downloadsList.get(i));
                        }
                    }
                    adapter.update(searched);
                }
                else {
                    searched = downloadsList;
                    adapter.update(downloadsList);
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        //updates actionbar title, resets episode search query, updates listview, updates nowplayingbar
        getSupportActionBar().setTitle("My Downloads");
        navigationView.getMenu().getItem(2).setChecked(true);
        epSearch.setText("");
        try {
            player.registerCallbacks(this);
        } catch (Exception e){
            //nada
        }
        adapter.update(downloadsList);
        nowPlayingView();
    }

    //longpress menu initialization
    public void showMenu(final Episode ep){
        final Dialog lpMenu = new Dialog(this);
        lpMenu.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lpMenu.setContentView(R.layout.longpress_episode);
        final int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        final int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        lpMenu.getWindow().setLayout((int)(width*.9),(int)(height*.9));

        setLpVariables(lpMenu);

        if (isInFavorites(ep)){
            lpFavorite.setText("Remove from Favorites");
        }
        else {
            lpFavorite.setText("Add to Favorites");
        }

        if (isInDownloads(ep)){
            lpDownload.setText("Delete Downloaded Episode");
        }
        else {
            lpDownload.setText("Download Episode");
        }

        lpPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentEpisode = ep;
                Intent intent = new Intent(Downloads.this, NowPlaying.class);
                intent.putExtra("prevPlay", player.getPlaying().getMp3());
                startActivity(intent);
                lpMenu.dismiss();
            }
        });

        lpFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lpMenu.dismiss();
                adapter.update(downloadsList);
                if (updateFavorites(ep)) {
                    Toast.makeText(Downloads.this, "Episode Added to Favorites", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(Downloads.this, "Episode Removed from Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        lpDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lpMenu.dismiss();
                if (isInDownloads(ep)) {
                    deleteDownload(ep);
                    adapter.update(downloadsList);
                }
                else{
                    downloadEpisode(ep);
                }
            }
        });

        lpMenu.show();
    }

    //callbacks
    @Override
    public void cbSetPlay(boolean play){
        if (play){
            nowPlayingButton.setImageResource(R.drawable.ic_baseline_pause_24px);
        }
        else {
            nowPlayingButton.setImageResource(R.drawable.ic_baseline_play_arrow_24px);
        }
    }

    @Override
    public void cbOnLoad(){
        //nada
    }

    @Override
    public void cbPreLoad(){
        //nada
    }
}

