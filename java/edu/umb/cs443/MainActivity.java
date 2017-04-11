package edu.umb.cs443;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback{

	public final static String DEBUG_TAG="edu.umb.cs443.MYMSG";
    private TextView textView;
    private EditText editText;
    private GoogleMap mMap;
    private JSONObject job;
    private String UserInput;
    private String url = "";
    private String stringUrl;
    private double lat;
    private double lng;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mFragment.getMapAsync(this);
        textView = (TextView)findViewById(R.id.textView);
        editText = (EditText)findViewById(R.id.editText);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public String StringUrl(String url) {
        return stringUrl ="http://api.openweathermap.org/data/2.5/weather?zip=" + url + ",us&appid=f9a0da7858696d1453d0faa23006c2d9";
    }

    public void getWeatherInfo(View v) {
        //Before connecting to internet check for URL
        ImageView img=(ImageView) findViewById(R.id.imageView);
        img.setImageBitmap(null);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            //fetch data
            UserInput = editText.getText().toString();
            url = StringUrl(UserInput);
            new GoogleAsyncTask().execute(url);
        } else {
            //display error
            Toast.makeText(getApplicationContext(), "No network connection available", Toast.LENGTH_SHORT);
        }



    }



    @Override
    public void onMapReady(GoogleMap map) {
        this.mMap=map;

    }

    private class GoogleAsyncTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urls) {

            try {
                return downloadUrl(urls[0]);
            } catch (Exception e) {
                return null;
            }
        }




        protected void onPostExecute(String result) {
            if(result!=null) textView.setText(result + "C");
            else { Log.i(DEBUG_TAG, "Result is null"); }
            try {
                job.getJSONArray("weather").getJSONObject(0).getString("icon");
                String file = "http://openweathermap.org/img/w/" + job.getJSONArray("weather").getJSONObject(0).getString("icon") + ".png";
                new IconAsyncTask().execute(file);
            }catch (JSONException e) {
                Log.i(DEBUG_TAG, e.toString());
            }

            // UPDATE THE FINAL UI IMMEDIATELY AFTER THE TASK IS EXECUTED

        }

    }
    //converts inputstream to string
    static String readStream(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        int len = 500;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // Starts the query
            conn.connect();

            int response = conn.getResponseCode();
            Log.i(DEBUG_TAG, "The response is: " + response);

            is = conn.getInputStream();
            if (is == null) {
                return null;
            }

            InputStream in = new BufferedInputStream(is);

            String input = readIt(in, len);

            job = new JSONObject(input);

            double d = job.getJSONObject("main").getDouble("temp") - 273.5;
            return String.format("%.2f", d);


        } catch (Exception e) {
            Log.i(DEBUG_TAG, e.toString());
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return null;
    }

    private class IconAsyncTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadIcon(urls[0]);
            } catch (IOException e) {
                return null;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView img=(ImageView) findViewById(R.id.imageView);
            if(result!=null) img.setImageBitmap(result);
            else{
                Log.i(DEBUG_TAG, "returned bitmap is null");}
            try {
                lat = job.getJSONObject("coord").getDouble("lat");
                lng = job.getJSONObject("coord").getDouble("lon");
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lng));
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(12);
                mMap.moveCamera(center);
                mMap.animateCamera(zoom);
            }catch (JSONException e) {
                Log.i(DEBUG_TAG, e.toString());
            }
        }
    }

    private Bitmap downloadIcon(String myurl) throws IOException {
        InputStream is = null;
        int len = 500;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.i(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            return bitmap;
        }catch(Exception e) {
            Log.i(DEBUG_TAG, e.toString());
        }finally {
            if (is != null) {
                is.close();
            }
        }

        return null;
    }

}


