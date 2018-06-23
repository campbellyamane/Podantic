package com.campbellyamane.podantic;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends General implements PodcastService.Callbacks{

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

    //Binding this Client to the AudioPlayer Service
    public static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PodcastService.LocalBinder binder = (PodcastService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PermissionCheck.readAndWriteExternalStorage(this);
        overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_from_right);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent playerIntent = new Intent(getBaseContext(), PodcastService.class);
        bindService(playerIntent, serviceConnection, 0);
        startService(playerIntent);

        podcasts = new ArrayList<Podcast>();

        //setting lists and views
        results =  new ArrayList<>();
        Log.d("PodAntic", results.toString());
        adapter = new AutoCompleteAdapter (this, android.R.layout.simple_dropdown_item_1line, results);
        searchView = (AutoCompleteTextView) findViewById(R.id.search);
        searchView.setDropDownBackgroundResource(R.color.black);
        searchView.setAdapter(adapter);

        searchView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    ps.cancel(true);
                } catch (Exception e){
                    //nothing
                }
                String se = s.toString();
                if (se.length() > 0) {
                    ps = new podSearch().execute(se);
                }

            }
        });
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
        try {
            player.registerCallbacks(this);
        } catch (Exception e){
            //nada
        }
        getSupportActionBar().setTitle("My Subscriptions");
        navigationView.getMenu().getItem(0).setChecked(true);
        nowPlayingView();

        updateCategories();

        if (player != null && !player.exists()){
            Intent svc = new Intent(getBaseContext(), PodcastService.class);
            bindService(svc, serviceConnection, 0);
        }

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

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Dialog lpMenu = new Dialog(MainActivity.this);
                lpMenu.requestWindowFeature(Window.FEATURE_NO_TITLE);
                lpMenu.setContentView(R.layout.longpress_podcast);
                final int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                final int height = Resources.getSystem().getDisplayMetrics().heightPixels;
                lpMenu.getWindow().setLayout((int)(width*.9),(int)(height*.9));

                TextView unsubscribe = (TextView) lpMenu.findViewById(R.id.unsubscribe);

                unsubscribe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "Unsubscribed from " + displayList.get(position).getName(), Toast.LENGTH_SHORT).show();
                        Log.d("PodAntic", Integer.toString(position));
                        Log.d("PodAntic", displayList.get(position).getName());
                        checkCategories(false, displayList.get(position));
                        subscriptionList.remove(displayList.get(position));
                        storageUtil.storeSubscriptions(subscriptionList);
                        podcastAdapter.update(displayList);
                        updateCategories();
                        lpMenu.dismiss();
                    }
                });

                lpMenu.show();

                return true;
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

                results =  new ArrayList<>();
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

    public void updateCategories(){
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
        Log.d("PodAntic", spinnerArray.toString());
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
