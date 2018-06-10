package com.campbellyamane.podantic;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends General {

    private ArrayList<String> results;
    private ArrayList<String> feeds;
    private ArrayList<String> artwork;
    private ArrayList<ArrayList<String>> categories;
    private AutoCompleteAdapter adapter;
    private AutoCompleteTextView searchView;
    private AsyncTask ps;

    private ArrayList<Podcast> podcasts;
    private PodcastAdapter podcastAdapter;
    private GridView gridView;

    private Spinner spinner;
    private ArrayList<String> spinnerArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        podcasts = new ArrayList<Podcast>();

        //setting lists and views
        results =  new ArrayList<>();
        adapter = new AutoCompleteAdapter (this, android.R.layout.simple_dropdown_item_1line, results);
        searchView = (AutoCompleteTextView) findViewById(R.id.search);
        searchView.setAdapter(adapter);

        //start single podcast activity on click
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PodHome.class);
                searchView.setText("");
                currentPodcast = new Podcast(artwork.get(position), results.get(position), feeds.get(position), categories.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        podcastAdapter = new PodcastAdapter(this, displayList);
        gridView = (GridView) findViewById(R.id.podcast_grid);
        gridView.setAdapter(podcastAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ps != null) {
                    ps.cancel(true);
                }
                Intent intent = new Intent(getApplicationContext(), PodHome.class);
                intent.putExtra("feed", displayList.get(position).getFeed());
                searchView.setText("");
                currentPodcast = displayList.get(position);
                startActivity(intent);
            }
        });

        spinnerArray =  new ArrayList<String>();
        for(Map.Entry<String,Integer> entry : categoriesList.entrySet()) {
            if (entry.getValue() > 0) {
                spinnerArray.add(entry.getKey() + " (" + entry.getValue() + ")");
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) findViewById(R.id.categories);
        sItems.setAdapter(adapter);

        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String curCat = spinnerArray.get(position).split(" \\(")[0];
                if (curCat.equals("All Categories")){
                    displayList = (ArrayList<Podcast>) subscriptionList;
                }
                else {
                    displayList = new ArrayList<>();
                    for (int i = 0; i < subscriptionList.size(); i++) {
                        if (subscriptionList.get(i).getCategories().contains(curCat)) {
                            displayList.add(subscriptionList.get(i));
                        }
                    }
                    Log.d("PodAntic", Integer.toString(displayList.size()));
                }
                podcastAdapter.update(displayList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //searching iTunes database as long as search is not empty
                if (s.toString().length() > 0) {
                    ps = new podSearch().execute(s.toString());
                }

            }
        });
    }

    public class podSearch extends AsyncTask<String, String, String> {
        String url1 = "http://itunes.apple.com/search?entity=podcast&limit=5&sort=popularity&term=";
        String mp3 = "";
        String title = "";
        @Override
        protected String doInBackground(String... query) {
            String search = query[0].replace("+", " ").replace("&", " ");
            Log.d("PodAntic", "podcastAsync");
            URL url = null;
            try {
                url = new URL(url1 + search);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                Log.d("PodAntic", "podcastURL");
                urlConnection.setReadTimeout(3000);
                InputStream in = urlConnection.getInputStream();
                Log.d("PodAntic", "podcastStream");
                InputStreamReader reader = new InputStreamReader(in);
                JSONObject json = getObject(reader);
                JSONArray searchResults = json.getJSONArray("results");
                Log.d("PodAntic", "gotJSON");
                results =  new ArrayList<>();
                feeds = new ArrayList<>();
                artwork = new ArrayList<>();
                categories = new ArrayList<>();

                //return top 5 results from iTunes db
                for (int i = 0; i < searchResults.length(); i++){
                    Log.d("PodAntic", searchResults.getJSONObject(i).getString("collectionName"));
                    results.add(searchResults.getJSONObject(i).getString("collectionName"));
                    feeds.add(searchResults.getJSONObject(i).getString("feedUrl"));
                    artwork.add(searchResults.getJSONObject(i).getString("artworkUrl100"));
                    JSONArray genres = searchResults.getJSONObject(i).getJSONArray("genres");
                    Log.d("PodAntic", genres.toString());
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
    }

    @Override
    protected void onPause(){
        super.onPause();
        results =  new ArrayList<>();
        feeds = new ArrayList<>();
        artwork = new ArrayList<>();
        try {
            ps.cancel(true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }
}
