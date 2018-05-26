package edu.iu.imomohimail.crwdin5;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {



    public int updateTimeDuration = 1000;
    public float updatemeterDuration = (float) 0.76;
    private GoogleMap mMap;

    LocationManager mLocationManager;
    LocationListener mLocationListener;

    private GoogleApiClient googleApiClient;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTimeDuration, updatemeterDuration, mLocationListener);
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    updateMape(lastKnownLocation);
                }
            }
        }
    }

    public void updateMape(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 20));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        createGoogleApi();








    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());


                mMap.animateCamera(CameraUpdateFactory.newLatLng(userLocation));
                final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(),location.getLongitude());
                ParseUser.getCurrentUser().put("currentLocation", parseGeoPoint);
                ParseUser.getCurrentUser().saveInBackground();

                creatCrowd(parseGeoPoint);



            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
     /*   if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }*/

        if (Build.VERSION.SDK_INT < 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTimeDuration, updatemeterDuration, mLocationListener);
            }

        }else {

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }else {

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTimeDuration, updatemeterDuration, mLocationListener);

                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation!= null){
                    updateMape(lastKnownLocation);
                }
            }
        }

        mMap.setMyLocationEnabled(true);




    }


    public void creatCrowd(final ParseGeoPoint GeoPoint){

                ParseQuery<ParseObject> query =  ParseQuery.getQuery("Crowds");
                query.whereNear("location",GeoPoint);
                query.setLimit(10);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e==null) {

                            if (objects.size() > 0) {
                                // if user is in a crowd , check if they still there else create another cround around that user
                                for (ParseObject object : objects) {
                                  ParseGeoPoint crowdloaction= (ParseGeoPoint) object.get("location");
                                  LatLng crowdpoint = new LatLng(crowdloaction.getLatitude(), crowdloaction.getLongitude());
                                  int crowdnumber= (int) object.get("poeple");
                                  mMap.addMarker(new MarkerOptions().position(crowdpoint).title(String.valueOf(crowdnumber)));

                                }

                            } else {

                                ParseObject request = new ParseObject("Crowds");
                                request.put("poeple", 1);
                                request.put("location", GeoPoint);

                                request.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e==null){
                                            //do something after saving to crowd data base
                                        }
                                    }
                                });
                                String id = request.getObjectId();
                                ParseUser.getCurrentUser().put("CrowdID",id);
                                ParseUser.getCurrentUser().saveInBackground();

                            }
                        }
                    }

                });





    }
    public void createGeofence(){

    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d("test", "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                   // .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }
}
