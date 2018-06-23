package com.campbellyamane.podantic;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Discover extends General implements PodcastService.Callbacks {

    private ArrayList<String> results;
    private ArrayList<String> feeds;
    private ArrayList<String> artwork;
    private ArrayList<String> descriptions;
    private ArrayList<ArrayList<String>> categories;
    private AutoCompleteAdapter adapter;
    private AutoCompleteTextView searchView;
    private AsyncTask cs;

    private ArrayList<Podcast> podcasts;
    private DiscoverAdapter discoverAdapter;
    private ListView listView;

    private ArrayList<String> spinnerArray;
    private LinkedHashMap<String, String> discoverCats;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        discoverCats = storageUtil.loadDiscover();

        dialog = new ProgressDialog(Discover.this);
        dialog.setTitle("Loading");
        dialog.setMessage("Grabbing Podcasts. Please wait...");

        //setting lists and views
        results =  new ArrayList<>();
        adapter = new AutoCompleteAdapter (this, android.R.layout.simple_dropdown_item_1line, results);
        searchView = (AutoCompleteTextView) findViewById(R.id.search);
        searchView.setDropDownBackgroundResource(R.color.black);
        searchView.setAdapter(adapter);

        searchView.addTextChangedListener(new TextWatcher() {
            Timer timer;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                timer = new Timer();
                timer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        Discover.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timer.cancel();
                                try {
                                    adapter.clear();
                                    cs.cancel(true);
                                } catch (Exception e){
                                    //nothing
                                }
                                String se = s.toString();
                                if (se.length() > 0) {
                                    cs = new Discover.podSearch().execute(se);
                                }

                            }
                        });
                    }
                }, 250);

            }
        });
        //start single podcast activity on click
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PodHome.class);
                searchView.setText("");
                currentPodcast = new Podcast(artwork.get(position), results.get(position), feeds.get(position), categories.get(position));
                Log.d("PodAntic",results.get(position));
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onResume(){
        super.onResume();
        try {
            player.registerCallbacks(this);
        } catch (Exception e){
            //nada
        }
        getSupportActionBar().setTitle("Discover");
        navigationView.getMenu().getItem(3).setChecked(true);
        nowPlayingView();

        setCategories();

        podcasts = new ArrayList<Podcast>();

        discoverAdapter = new DiscoverAdapter(this, podcasts);
        listView = (ListView) findViewById(R.id.podcast_list);
        listView.setAdapter(discoverAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (cs != null) {
                    cs.cancel(true);
                }
                Intent intent = new Intent(getApplicationContext(), PodHome.class);
                intent.putExtra("feed", podcasts.get(position).getFeed());
                searchView.setText("");
                currentPodcast = podcasts.get(position);
                startActivity(intent);
            }
        });

        /*

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Dialog lpMenu = new Dialog(Discover.this);
                lpMenu.requestWindowFeature(Window.FEATURE_NO_TITLE);
                lpMenu.setContentView(R.layout.longpress_podcast);
                final int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                final int height = Resources.getSystem().getDisplayMetrics().heightPixels;
                lpMenu.getWindow().setLayout((int)(width*.9),(int)(height*.9));

                lpMenu.show();

                return true;
            }
        });
        */
    }

    public void setCategories(){
        spinnerArray =  new ArrayList<String>();
        for(Map.Entry<String, String> entry : discoverCats.entrySet()) {
            spinnerArray.add(entry.getKey());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) findViewById(R.id.categories);
        sItems.setAdapter(adapter);
        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dialog.show();
                cs = new discoverSearch().execute(discoverCats.get(spinnerArray.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public class podSearch extends AsyncTask<String, String, String> {
        String url1 = "https://itunes.apple.com/search?entity=podcast&limit=5&sort=popularity&term=";
        String mp3 = "";
        String title = "";
        @Override
        protected String doInBackground(String... query) {
            String search = query[0].replace("+", " ").replace("&", " ");
            Log.d("PodAntic", "podcastAsync");
            URL url = null;
            try {

                url = new URL(url1 + search);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                Log.d("PodAntic", "podcastURL");
                InputStream in = (InputStream) conn.getInputStream();
                Log.d("PodAntic", "podcastStream");
                InputStreamReader reader = new InputStreamReader(in);
                JSONObject json = getObject(reader);
                JSONArray searchResults = json.getJSONArray("results");
                Log.d("PodAntic", "gotJSON");

                results = new ArrayList<>();
                feeds = new ArrayList<>();
                artwork = new ArrayList<>();
                categories = new ArrayList<>();

                //return top 5 results from iTunes db
                for (int i = 0; i < searchResults.length(); i++){
                    results.add(searchResults.getJSONObject(i).getString("collectionName"));
                    feeds.add(searchResults.getJSONObject(i).getString("feedUrl"));
                    artwork.add(searchResults.getJSONObject(i).getString("artworkUrl100"));
                    JSONArray genres = searchResults.getJSONObject(i).getJSONArray("genres");
                    ArrayList<String> cats = new ArrayList<>();
                    for (int j = 0; j < genres.length(); j++){
                        cats.add(genres.getString(j));
                    }
                    categories.add(cats);
                    if (i == 4){
                        break;
                    }
                }

                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d("PodAntic", "malformedurl");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("PodAntic", "input");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("PodAntic", "json");
            }
            return title;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //Update the progress of current task
        }

        @Override
        protected void onPostExecute(String t) {
            adapter.update(results);
        }
    }

    public class discoverSearch extends AsyncTask<String, String, String> {
        String url1 = "https://itunes.apple.com/search?term=podcast&genreId=";
        String url2 = "&limit=200&sort=popularity";
        String mp3 = "";
        String title = "";
        @Override
        protected String doInBackground(String... query) {
            String search = url1+ query[0] + url2;
            Log.d("PodAntic", "podcastAsync");
            URL url = null;
            try {
                url = new URL(search);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                Log.d("PodAntic", "podcastURL");
                InputStream in = (InputStream) conn.getInputStream();
                Log.d("PodAntic", "podcastStream");
                InputStreamReader reader = new InputStreamReader(in);
                JSONObject json = getObject(reader);
                JSONArray searchResults = json.getJSONArray("results");
                Log.d("PodAntic", "gotJSON");

                podcasts = new ArrayList<Podcast>();

                //return results from iTunes db
                for (int i = 0; i < searchResults.length(); i++){
                    String name = searchResults.getJSONObject(i).getString("collectionName");
                    String feed = searchResults.getJSONObject(i).getString("feedUrl");
                    String art = searchResults.getJSONObject(i).getString("artworkUrl100");
                    String artist = searchResults.getJSONObject(i).getString("artistName");
                    JSONArray genres = searchResults.getJSONObject(i).getJSONArray("genres");
                    ArrayList<String> cats = new ArrayList<>();
                    for (int j = 0; j < genres.length(); j++){
                        cats.add(genres.getString(j));
                    }
                    podcasts.add(new Podcast(art, name, feed, cats, artist));
                }

                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d("PodAntic", "malformedurl");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("PodAntic", "input");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("PodAntic", "json");
            }
            return title;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //Update the progress of current task
        }

        @Override
        protected void onPostExecute(String t) {
            listView.smoothScrollToPosition(0);
            dialog.dismiss();
            discoverAdapter.update(podcasts);
        }
    }

    public JSONObject getObject(InputStreamReader in) throws IOException, JSONException {
        BufferedReader bR = new BufferedReader(in);
        String line = "";

        StringBuilder responseStrBuilder = new StringBuilder();
        while((line =  bR.readLine()) != null){

            responseStrBuilder.append(line);
        }

        JSONObject result= new JSONObject(responseStrBuilder.toString());
        return result;
    }

    @Override
    protected void onPause(){
        super.onPause();
        results =  new ArrayList<>();
        feeds = new ArrayList<>();
        artwork = new ArrayList<>();
        categories = new ArrayList<>();
        try {
            cs.cancel(true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void cbSetPlay(boolean play){
        Log.d("PodAntic", "callback");
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
