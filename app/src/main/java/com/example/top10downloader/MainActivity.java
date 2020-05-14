package com.example.top10downloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private String auxUrl;
    private int feedLimit = 10;
    private int auxLimit;
    private static final String SAVED_LIMIT = "feedLimit";
    private static final String SAVED_URL = "feedUrl";
    //private String feedType = "freeapplications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listApps = findViewById(R.id.xmlListView);

        if(savedInstanceState != null){
            feedLimit = savedInstanceState.getInt(SAVED_LIMIT);
            feedUrl = savedInstanceState.getString(SAVED_URL);
        }

        downloadUrl(String.format(feedUrl, feedLimit));
        Log.d(TAG, "saved url: " + SAVED_URL);
        Log.d(TAG, "saved limit: " + SAVED_LIMIT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if(feedLimit == 10){
            menu.findItem(R.id.menu10).setChecked(true);
        }else{
            menu.findItem(R.id.menu25).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, auxUrl);
        Log.d(TAG, feedUrl);


        switch(id){
            case R.id.menuRefresh:
                downloadUrl(String.format(feedUrl, feedLimit));
                break;
            case R.id.menuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.menuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.menuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.menu10:
            case R.id.menu25:
                if(!item.isChecked()){
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                }break;
            default:
                return super.onOptionsItemSelected(item);
        }
        if(!auxUrl.equals(feedUrl) || feedLimit != auxLimit) {
            downloadUrl(String.format(feedUrl, feedLimit));
        }
        return true;
    }

    private void downloadUrl(String feedUrl){
        Log.d(TAG, "onCreate: starting Asynctask");
        DonwloadData donwloadData = new DonwloadData();
        donwloadData.execute(feedUrl);
        auxUrl = this.feedUrl;
        auxLimit = this.feedLimit;
        Log.d(TAG, "onCreate:  done");
    }

    private class DonwloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DonwloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute: parameter is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<FeedEntry>(MainActivity.this,
//                    R.layout.item_list, parseApplications.getApplication());
//            listApps.setAdapter(arrayAdapter);
            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record,
                    parseApplications.getApplication());
            listApps.setAdapter(feedAdapter);
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            String rssFeed = downloadXML(strings[0]);
            if(rssFeed == null){
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        private String downloadXML (String urlPath){
            StringBuilder xmlResult = new StringBuilder();

            try{
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: The response code was " + response);
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader reader = new BufferedReader(inputStreamReader);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                int charsRead;
                char[] inputBuffer = new char[500];
                while (true){
                    charsRead = reader.read(inputBuffer);
                    if(charsRead < 0){
                        break;
                    }
                    if(charsRead > 0){
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();
                return xmlResult.toString();

            }catch (MalformedURLException e){
                Log.e(TAG, "downloadXML: Invalid URL " + e.getMessage());
            }catch (IOException e){
                Log.e(TAG, "downloadXML: IO Exception reading data " + e.getMessage());
            }catch (SecurityException e){
                Log.e(TAG, "downloadXML: Security Exception. Need Permission." + e.getMessage() );
//                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        outState.putInt(SAVED_LIMIT, feedLimit);
        outState.putString(SAVED_URL, feedUrl);
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
