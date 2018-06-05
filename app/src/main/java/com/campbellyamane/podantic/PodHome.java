package com.campbellyamane.podantic;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class PodHome extends AppCompatActivity {
    private ImageView img;
    private TextView name;
    private ListView listView;
    private ArrayList<Episode> episodes;
    private EpisodeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pod_home);
        Intent intent = getIntent();
        String feed = intent.getStringExtra("feed");

        //get episodes for podcast
        new podRetrieve().execute(feed);

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

                startActivity(i);
            }
        });
    }

    public class podRetrieve extends AsyncTask<String, String, Document> {
        String title = "";
        Document doc;
        @Override
        protected Document doInBackground(String... f) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(new URL(f[0]).openStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
                Node image = d.getElementsByTagName("url").item(0).getChildNodes().item(0);
                podImage = image.getTextContent();
                Picasso.get().load(podImage).fit().into(img);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            NodeList eps = d.getElementsByTagName("item");
            name.setText(title.getTextContent());
            Log.d("PodAntic", Integer.toString(eps.getLength()));
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
                    String eArt = "";
                    try {
                        eArt = img.getAttribute("href");
                    } catch (Exception ex) {
                        eArt = podImage;
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
        }
    }
}
