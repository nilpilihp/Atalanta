package com.example.atalanta;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.List;
import java.util.Random;


public class MainActivity extends FragmentActivity implements  OnMapReadyCallback{
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 12;
    private RequestQueue requestQueue;
    private String url = "https://www.mapquestapi.com/directions/v2/alternateroutes";
    private static final String TAG = "ApiRequest";
    private final int[] colors = {Color.RED,Color.BLUE,Color.DKGRAY};
    private LatLng mLastKnownLatLng = new LatLng(37.4220, -122.0841); // subject to update
    private UiSettings mUiSettings;

    // VARIABLES FOR TESTING
    private final LatLng googlePlex = new LatLng(37.4220, -122.0841); // google plex
    private final LatLng applePark  = new LatLng(37.3349, -122.0091); // apple park
    Button test_button;

    //Variables for navigation bar
    private Button health, profile;
    private Spinner miles;
    private Integer[] mileOptions;
    private static Integer selectedMileage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment.getMapAsync(this);

        // Navigation bar with buttons and scroller on bottom of screen
        //Get reference of button
        health = (Button) findViewById(R.id.navHealth);
        //Perform setOnClickListener on first button
        health.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_health = new Intent(getApplicationContext(), HealthActivity.class);
                startActivity(intent_health);
            }
        });

        profile = (Button) findViewById(R.id.navProfile);
        profile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });

        mileOptions = new Integer[20];
        for(int i = 1; i <= 20; i++){
            mileOptions[i-1] = i;
        }
        Spinner miles = (Spinner) findViewById(R.id.milesNum);
        miles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMileage = Integer.parseInt(parent.getItemAtPosition(position).toString());
                //Check that mileage variable is updated
                Log.d("MILEAGE: ", selectedMileage.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMileage = 1;
            }
        });

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getApplicationContext(), android.R.layout.simple_spinner_item, mileOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        miles.setAdapter(adapter);

        //Test button for accessing login page, navbar onclick implemented
        test_button = (Button) findViewById(R.id.test_button);
        test_button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(applePark));
                sendAndRequestResponse(applePark);
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // map settings
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setAllGesturesEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);
        displayMyLocation(); // called in getMapAsync since want map ready then display current marker
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // start clean
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(mLastKnownLatLng));

                // Add new marker to the Google Map Android API V2
                mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // Get those dank routes
                sendAndRequestResponse(latLng);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]grantResults)
    {
        if(requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        {
            // if request is cancelled, result arrays are empty
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                displayMyLocation();
        }
    }

    private void displayMyLocation() {
        // Check if permission granted
        int permission = ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        // if not, ask for permission
        if(permission == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        // if granted, display marker at current location
        else
        {
            mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(this,task ->
            {
                Location mLastKnownLocation = task.getResult();
                if(task.isSuccessful() && mLastKnownLocation != null)
                {
                    mLastKnownLatLng = new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude());
                    // move camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mLastKnownLatLng));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                    // add current position marker
                    mMap.addMarker(new MarkerOptions().position(mLastKnownLatLng));
                }
            });
        }
    }

    // function to get route direction
    private void sendAndRequestResponse(LatLng dest) {
        // Check if permission granted
        int permission = ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        // if not, ask for permission
        if(permission == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        // if granted
        else
        {
            mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(this,task ->
            {
                Location mLastKnownLocation = task.getResult();
                if(task.isSuccessful() && mLastKnownLocation != null)
                {
                    LatLng mLastKnownLatLng = new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude());
                    // Action
                    // RequestQueue initialized
                    requestQueue = Volley.newRequestQueue(this);
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                            (Request.Method.GET, getDirectionsUrl(mLastKnownLatLng,dest), null, new Response.Listener<JSONObject>() {
                                // use googleplex as backup origin
                                @Override
                                public void onResponse(JSONObject response) {
                                    // Do the heavy duty of parsing json on async task
                                    new parseDrawWorker().execute(response);
                                }
                            }, error -> Log.d(TAG,"Error: "+ error.toString()));
                    requestQueue.add(jsonObjectRequest);
                }
            });
        }

    }
    // helper function for constructing the API query
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // API Key
        String str_key = "key=" + getString(R.string.direction_api_key);
        // Origin of route
        String str_origin = "from=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "to=" + dest.latitude + "," + dest.longitude;
        // routeType
        String str_type = "routeType=" + "pedestrian";
        // maxRoute set to 3
        String str_max = "maxRoutes=" + "3";
        // Building the parameters to the web service
        String parameters = str_key + "&"+ str_origin + "&" + str_dest + "&" + str_type + "&" + str_max;

        // Building the url to the web service
        String final_url = url + "?" + parameters;

        return final_url;
    }

    private class parseDrawWorker extends AsyncTask<JSONObject , Void, List<List<List<LatLng>>> > {
        @Override
        protected List<List<List<LatLng>>> doInBackground(JSONObject... jsonObjects) {
            JSONObject response = jsonObjects[0];
            DirectionsJSONParser parser = new DirectionsJSONParser();
            return parser.parse(response);
        }
        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
        }
        protected void onPostExecute(List<List<List<LatLng>>> dataWrapper) {
            // First field in wrapper is distances of each route
            Log.i(TAG, "distance list " + dataWrapper.get(0).toString());
            // Second field in wrapper is list of routes, each route contains a list of wayPoints
            List<List<LatLng>> routes =  dataWrapper.get(1);
            Random rnd = new Random(1);
            for (int i = 0; i< routes.size(); i++){
                List<LatLng> route = routes.get(i);
                // use the first route's bounds to set map box
                if(i == 0){
                    // move camera and bounding box so account for nav bar at bottom
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                            new LatLngBounds(route.remove(0),route.remove(0)), 200));// second remove get's LatLng NEBound
                }
                else{
                    // don't care about bound box for other routes
                    route.remove(0);
                    route.remove(0);
                }
                // add polyLine with random color with the LatLng points received in routes
                mMap.addPolyline(new PolylineOptions().clickable(true).color(colors[i]).addAll(route));
            }
        }
    }
}


