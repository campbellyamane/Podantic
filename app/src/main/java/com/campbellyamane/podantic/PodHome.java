package com.campbellyamane.podantic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class PodHome extends General {
    private ImageView img;
    private TextView name;
    private Button subscribe;
    private ListView listView;
    private ArrayList<Episode> episodes;
    private EpisodeAdapter adapter;
    private AsyncTask es;
    private ProgressDialog dialog;
    private EditText epSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pod_home);
        dialog = ProgressDialog.show(PodHome.this, "Loading",
                "Grabbing Episodes. Please wait...", true);
        Intent intent = getIntent();

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
                Intent i = new Intent(PodHome.this, NowPlaying.class);
                i.putExtra("episode",episodes.get(position).getTitle());
                i.putExtra("art",episodes.get(position).getArt());
                i.putExtra("podcast",episodes.get(position).getPodcast());
                i.putExtra("mp3",episodes.get(position).getMp3());
                Log.d("PodAntic", player.getPlaying().getMp3());
                Log.d("PodAntic", episodes.get(position).getMp3());
                if (!player.getPlaying().getMp3().equals(episodes.get(position).getMp3())) {
                    player.playMedia(episodes.get(position));
                }

                startActivity(i);
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
                if (s.toString().length() > 0) {
                    ArrayList<Episode> searched = new ArrayList<Episode>();
                    for (int i = 0; i < episodes.size(); i++) {
                        if (episodes.get(i).getTitle().contains(s) || episodes.get(i).getDetails().contains(s)) {
                            searched.add(episodes.get(i));
                        }
                    }
                    adapter.update(searched);
                }
                else {
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
                    checkCategories(false);
                }
                else{
                    subscriptionList.add(currentPodcast);
                    storageUtil.storeSubscriptions(subscriptionList);
                    subscribe.setText("Unsubscribe");
                    subscribe.setTextColor(Color.parseColor("red"));
                    checkCategories(true);
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
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(f[0]).openConnection();
                urlConnection.setReadTimeout(3000);
                InputStream in = urlConnection.getInputStream();
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

    public void checkCategories(Boolean newSub){
        ArrayList<String> currentCats = currentPodcast.getCategories();
        if (newSub){
            try {
                categoriesList.put("All Categories", categoriesList.get("All Categories") + 1);
            } catch (Exception e){
                categoriesList.put("All Categories", 1);
            }
            for (int i = 0; i < currentCats.size(); i++){
                String key = currentCats.get(i);
                if (categoriesList.containsKey(key)){
                    categoriesList.put(key, categoriesList.get(key) + 1);
                }
                else{
                    categoriesList.put(key, 1);
                }
            }
        }
        else{
            for (int i = 0; i < currentCats.size(); i++) {
                String key = currentCats.get(i);
                categoriesList.put(key, categoriesList.get(key) - 1);
            }
            categoriesList.put("All Categories", categoriesList.get("All Categories") - 1);
        }
        storageUtil.storeCategories(categoriesList);
        categoriesList = storageUtil.loadCategories();
    }
}
