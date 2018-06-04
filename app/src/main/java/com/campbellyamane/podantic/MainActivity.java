package com.campbellyamane.podantic;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> results;
    private ArrayList<String> feeds;
    private AutoCompleteAdapter adapter;
    private AutoCompleteTextView searchView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        results =  new ArrayList<>();
        adapter = new AutoCompleteAdapter (this, android.R.layout.simple_dropdown_item_1line, results);
        searchView = (AutoCompleteTextView) findViewById(R.id.search);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString() != "") {
                    new podSearch().execute(s.toString());
                }

            }
        });
        searchView.setAdapter(adapter);

        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PodHome.class);
                intent.putExtra("feed", feeds.get(position));
                searchView.setText("");
                startActivity(intent);
            }
        });
    }

    public class podSearch extends AsyncTask<String, String, String> {
        String url1 = "https://itunes.apple.com/search?entity=podcast&limit=5&sort=popularity&term=";
        String mp3 = "";
        String title = "";

        @Override
        protected String doInBackground(String... query) {
            URL url = null;
            String search = query[0].replace("&", "");
            search = search.replace("+", "");
            search = search.replace(" ", "+");
            try {
                url = new URL(url1 + search);
                InputStream in = url.openStream();
                InputStreamReader reader = new InputStreamReader(in);
                JSONObject json = getObject(reader);
                results =  new ArrayList<>();
                feeds = new ArrayList<>();
                JSONArray searchResults = json.getJSONArray("results");
                for (int i = 0; i < searchResults.length(); i++){
                    results.add(searchResults.getJSONObject(i).getString("collectionName"));
                    feeds.add(searchResults.getJSONObject(i).getString("feedUrl"));
                    if (i == 4){
                        break;
                    }
                }

                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("PodAntic", results.toString());
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
}
