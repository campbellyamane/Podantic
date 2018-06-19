package com.campbellyamane.podantic;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class PodHome extends General implements PodcastService.Callbacks{
    private ImageView img;
    private TextView name;
    private Button subscribe;
    private ListView listView;
    private ArrayList<Episode> episodes;
    private EpisodeAdapter adapter;
    private AsyncTask es;
    private ProgressDialog dialog;
    private EditText epSearch;
    private boolean bound;

    private ArrayList<Episode> searched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_from_right);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pod_home);

        dialog = new ProgressDialog(PodHome.this);
        dialog.setTitle("Loading");
        dialog.setMessage("Grabbing Episodes. Please wait...");
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                es.cancel(true);
                onBackPressed();
            }
        });
        dialog.show();
        //get episodes for podcast
        if (es != null){
            es.cancel(true);
        }
        es = new podRetrieve().execute(currentPodcast.getFeed());

        {
            img = (ImageView) findViewById(R.id.podimage);
            name = (TextView) findViewById(R.id.podinfo);
            listView = (ListView) findViewById(R.id.episodes);
        }

        //loading episodes into listview
        episodes = new ArrayList<Episode>();
        adapter = new EpisodeAdapter(this, episodes);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(PodHome.this, NowPlaying.class);
                if (searched != null && searched.size() != episodes.size()) {
                    currentEpisode = searched.get(position);
                }
                else{
                    currentEpisode = episodes.get(position);
                }
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (searched != null && searched.size() != episodes.size()) {
                    showMenu(searched.get(position));
                }
                else {
                    showMenu(episodes.get(position));
                }
                return true;
            }
        });

        epSearch = findViewById(R.id.episode_search);
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
                    for (int i = 0; i < episodes.size(); i++) {
                        if (episodes.get(i).getTitle().toLowerCase().contains(se) || episodes.get(i).getDetails().toLowerCase().contains(se)) {
                            searched.add(episodes.get(i));
                        }
                    }
                    adapter.update(searched);
                }
                else {
                    searched = episodes;
                    adapter.update(episodes);
                }
            }
        });

        subscribe = findViewById(R.id.subscribe);

        for (int i = 0; i < subscriptionList.size(); i++){
            if (subscriptionList.get(i).getName().equals(currentPodcast.getName())){
                subscribe.setText("Unsubscribe");
                subscribe.setTextColor(Color.parseColor("red"));
            }
        }

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (subscribe.getText().equals("Unsubscribe")) {
                    for (int i = 0; i < subscriptionList.size(); i++){
                        if (subscriptionList.get(i).getName().equals(currentPodcast.getName())){
                            subscriptionList.remove(i);
                        }
                    }
                    storageUtil.storeSubscriptions(subscriptionList);
                    subscribe.setText("Subscribe");
                    subscribe.setTextColor(Color.parseColor("white"));
                    checkCategories(false, currentPodcast);
                }
                else{
                    subscriptionList.add(currentPodcast);
                    storageUtil.storeSubscriptions(subscriptionList);
                    subscribe.setText("Unsubscribe");
                    subscribe.setTextColor(Color.parseColor("red"));
                    checkCategories(true, currentPodcast);
                }
            }
        });
    }

    public class podRetrieve extends AsyncTask<String, String, Document> {
        String title = "";
        Document doc;
        @Override
        protected Document doInBackground(String... f) {
            Log.d("PodAntic", "episodeAsync");
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                URLConnection conn = new URL(f[0]).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                InputStream in =  (InputStream) conn.getInputStream();
                doc = db.parse(in);
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                dialog.dismiss();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e){
                e.printStackTrace();
            }

            return doc;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //Update the progress of current task
        }

        @Override
        protected void onPostExecute(Document d) {
            Node title = d.getElementsByTagName("title").item(0).getChildNodes().item(0);
            String podImage = "";
            try {
                Element image = (Element) d.getElementsByTagName("itunes:image").item(0);
                podImage = image.getAttribute("href");
                Picasso.get().load(podImage).fit().into(img);
                currentPodcast.setArt(podImage);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            NodeList eps = d.getElementsByTagName("item");
            name.setText(title.getTextContent());
            findViewById(R.id.subscribe).setVisibility(View.VISIBLE);
            for (int i = 0; i < eps.getLength(); i++){
                try {
                    Element e = (Element) eps.item(i);
                    String eTitle = e.getElementsByTagName("title").item(0).getTextContent();
                    String eDate = e.getElementsByTagName("pubDate").item(0).getTextContent();
                    String eDetails;
                    try {
                        eDetails = e.getElementsByTagName("itunes:summary").item(0).getTextContent();
                    }
                    catch (Exception ex){
                        eDetails = e.getElementsByTagName("description").item(0).getTextContent();
                    }
                    String eTime = e.getElementsByTagName("itunes:duration").item(0).getTextContent();
                    Element img = (Element) e.getElementsByTagName("itunes:image").item(0);
                    String eArt = currentPodcast.getArt();
                    try {
                        eArt = img.getAttribute("href");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Element enc = (Element) e.getElementsByTagName("enclosure").item(0);
                    String eMp3 = enc.getAttribute("url");

                    episodes.add(new Episode(eTitle, eDate, eDetails, eMp3, eArt, eTime, title.getTextContent()));
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        try {
            es.cancel(true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        epSearch.setText("");
        try {
            player.registerCallbacks(this);
        } catch (Exception e){
            //nada
        }
        getSupportActionBar().setTitle(currentPodcast.getName());
        adapter.update(episodes);
        nowPlayingView();
    }

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
                Intent intent = new Intent(PodHome.this, NowPlaying.class);
                intent.putExtra("prevPlay", player.getPlaying().getMp3());
                startActivity(intent);
                lpMenu.dismiss();
            }
        });

        lpFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lpMenu.dismiss();
                if (updateFavorites(ep)) {
                    Toast.makeText(PodHome.this, "Episode Added to Favorites", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(PodHome.this, "Episode Removed from Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        lpDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lpMenu.dismiss();
                if (isInDownloads(ep)) {
                    deleteDownload(ep);
                }
                else{
                    downloadEpisode(ep);
                }
            }
        });

        lpMenu.show();
    }

    public Palette createPaletteSync() {
        BitmapDrawable drawable = (BitmapDrawable) img.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        Palette p = Palette.from(bitmap).generate();
        return p;
    }

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
