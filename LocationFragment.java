package checkboxsharedpreferences.moon.com.athenamap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Sarah on 2018-04-01.
 */

public class LocationFragment extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private TextView distanceValue;
    private TextView durationValue;
    private GoogleApiClient googleApiClient;
    //google map
    ArrayList<LatLng> MarkerPoints;
    //driving instruction
    private ListView mListView;
    private LatLng fromAddress;
    private String toAddress;
    private SupportMapFragment mapFragment;

    private static final int REQUEST_LOCATION = 2;
    ArrayList<LatLng> markerPoints;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mListView = (ListView) findViewById(R.id.driving_instruction);

        // Initializing
        MarkerPoints = new ArrayList<>();

        Bundle bundle = getIntent().getExtras();
        toAddress = (String) bundle.get("toAddress");

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        googleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public void mapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng location;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(43.467094, -80.520746) ,15) );
        MarkerOptions marker = new MarkerOptions().position(new LatLng(43.467094, -80.520746)).title("Athenasoftware");;
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.buboowlfinal));
        mMap.addMarker(marker);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
//                if(markerPoints.size() == 2) {
//                    markerPoints.clear();
//                    mMap.clear();
//                }
                markerPoints.add(latLng);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if(markerPoints.size() == 1) {
                    //first marker
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else{
                    //second marker
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);

                //show direction
                if(markerPoints.size() == 2){
                    LatLng origin = markerPoints.get(0);
                    LatLng dest = markerPoints.get(1);

                    // Getting URL to the Google Directions API
                    String url = getUrl(origin, dest);
                    Log.d("getRoute", url.toString());
                    //request get direction
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                }
            }


        });
    }

    public void getRoute(LatLng point) {
        // Adding new item to the ArrayList
        MarkerPoints.add(point);

        // Creating MarkerOptions
        MarkerOptions options = new MarkerOptions();

        // Setting the position of the marker
        options.position(point);

        /**
         * For the start location, the color of marker is GREEN and
         * for the end location, the color of marker is RED.
         */
        if (MarkerPoints.size() == 1) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        } else if (MarkerPoints.size() == 2) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }

        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options);

        // Checks, whether start and end locations are captured
        if (MarkerPoints.size() >= 2) {
            LatLng origin = MarkerPoints.get(0);
            LatLng dest = MarkerPoints.get(1);

            // Getting URL to the Google Directions API
            String url = getUrl(origin, dest);
            Log.d("getRoute", url.toString());
            FetchUrl FetchUrl = new FetchUrl();

            // Start downloading json data from Google Directions API
            FetchUrl.execute(url);
        }

    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";

        //when use transit mode, add mode, and current time
        //mode
        //String mode = "transit_mode=train|tram|subway";

        //time
        Date departure = new Date(); //get current time
        long departure_time = departure.getTime();
        String currentTime = "departure_time" + departure_time;

        // Building the parameters to the web service==> driving mode
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        //Building the parameters to the web service==> transit mode
        //String parameters = str_origin + "&" + str_dest + "&" + sensor+ "&" + mode+ "&" + currentTime;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    /**
     * A method to download json data from url
     * ownloadUrl is used to fecth url from web service and result is parsed using ParserTask (Async Task).
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location from = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        fromAddress = new LatLng(from.getLatitude(),from.getLongitude());
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mapReady(googleMap);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    // Fetches data from url passed
    /*
    * AsyncTask is an abstract class provided by Android which helps us to use the UI thread properly.
    * This class allows us to perform long/background operations and show its result on the UI thread without having to manipulate threads.
     * Here doInBackground task will be implemented in background and onPostExecute will be shown on GUI.
    * */
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);

            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }
    public void getDistAndDuration(JSONObject jObj){
        try{
            JSONArray jRoutes  = jObj.getJSONArray("routes");
            JSONObject jRoute = jRoutes .getJSONObject(0);
            JSONArray legs = jRoute.getJSONArray("legs");
            JSONObject leg  = legs.getJSONObject(0);
            JSONObject distance = leg.getJSONObject("distance");
            JSONObject duration = leg.getJSONObject("duration");
            JSONArray jSteps = null;
            JSONObject jStep;
            JSONArray steps = leg.getJSONArray("steps");

            JSONObject jLeg;

            final String textDist = distance.getString("text");
            final String textDur = duration.getString("text");

            final String[] listItems = new String[steps.length()];
            //Log.i("moon size", steps.length()+"");
            for (int k = 0; k < steps.length(); k++) {
                JSONObject step = steps.getJSONObject(k);
                String instruction = step.getString("html_instructions").replaceAll("<(.*?)*>", "");

                //Log.i("moon html_instruction", instruction);
                listItems[k] = instruction;
            }
            // Create an ArrayAdapter from List
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                    (this, android.R.layout.simple_list_item_1, listItems){
                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    /// Get the Item from ListView
                    View view = super.getView(position, convertView, parent);

                    TextView tv = (TextView) view.findViewById(android.R.id.text1);

                    // Set the text size 16 dip for ListView each item
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);

                    // Return the view
                    return view;
                }
            };
            //arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems);
            //Log.i("moon DISTANCE", ""+textDist);
            // Log.i("moon TIME", ""+textDur);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setRouteDistanceAndDuration(textDist, textDur);
                    mListView.setAdapter(arrayAdapter);


                }
            });


        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void setRouteDistanceAndDuration(String distance, String duration){
        distanceValue.setText(distance);
        durationValue.setText(duration);
    }
    /**
     * A class to parse the Google Places in JSON format
     */
//doInBackground will actually parse the data and onPostExecute will draw route on Google Maps.
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);

                //display distance and duration
                getDistAndDuration(jObject);


                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            String distance = "";
            String duration = "";
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);



                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    builder.include(position);
                    points.add(position);
                }


                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20));
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

}