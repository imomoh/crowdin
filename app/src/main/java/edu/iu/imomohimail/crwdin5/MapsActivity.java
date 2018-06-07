package edu.iu.imomohimail.crwdin5;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.location.DetectedActivity.STILL;
import static com.google.android.gms.location.DetectedActivity.TILTING;
import static com.google.android.gms.location.DetectedActivity.UNKNOWN;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {


    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 15.0f; // in meters

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    public int updateTimeDuration = 1000;
    public float updatemeterDuration = (float) 1;
    private GoogleMap mMap;

    private static final String TAG = "test";
    public int doItOnce=0;
    ArrayList<DetectedActivity> detectedActivities;
    List<Geofence> mGeofencesList = new ArrayList<Geofence>();
    List<Marker> mMarkersList;
    String UserOldCrowdID;
    boolean skip = true;

    List<ActivityTransition> transitions = new ArrayList<>();

    private ActivityRecognitionClient mActivityRecognitionClient;
    private Context mContext;


    LocationManager mLocationManager;
    LocationListener mLocationListener;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mContext = this;
        mapFragment.getMapAsync(this);

        createGoogleApi();

        detectedActivities = Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        Constants.KEY_DETECTED_ACTIVITIES, ""));

        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        startTransitinActivityRecognition();

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());


                final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

                updateDatabase(location);
                updateDetectedActivitiesList();
                int i = detectedActivities.size();
                for(int k=0;k<i;k++) {
                    if (detectedActivities.get(k).getType() == STILL
                            ||detectedActivities.get(k).getType() == TILTING
                            ||detectedActivities.get(k).getType() == UNKNOWN ) {


                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                        }else {
                            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            float results[] = new float[]{0,0,0};
                            Location.distanceBetween(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude(),
                                    location.getLatitude(),location.getLongitude(),results);

                            //if distance between the first object which is the closest is  greater than  30 then they are a new crowd
                            if (results[0]>30 ) {
                                // mMap.clear();
                                creatCrowd (parseGeoPoint);
                            }


                        }

                    }
                }
                if (doItOnce==0){
                    creatCrowd (parseGeoPoint);
                    doItOnce=1;
                }





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

        mMap.setMyLocationEnabled(true);

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
                    final ParseGeoPoint lastKnowparseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                  //  creatCrowd(lastKnowparseGeoPoint);
                }
            }
        }








    }


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


    public void creatCrowd(final ParseGeoPoint GeoPoint){

                ParseQuery<ParseObject> query =  ParseQuery.getQuery("Crowds");
                query.whereNear("location",GeoPoint);
                query.setLimit(10);
                query.orderByAscending("location");
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e==null) {
                            boolean thisisClosestLoction = true;
                            if (objects.size() > 0) {
                                // if user is in a crowd , check if they still there else create another cround around that user

                                // counter for first object
                                mMarkersList = new ArrayList<Marker>();

                                for (ParseObject object : objects) {
                                  ParseGeoPoint crowdloaction= (ParseGeoPoint) object.get("location");
                                  LatLng crowdpoint = new LatLng(crowdloaction.getLatitude(), crowdloaction.getLongitude());
                                  int crowdnumber= (int) object.get("poeple");
                                  mMap.addMarker(new MarkerOptions().position(crowdpoint).title(String.valueOf(crowdnumber)));

                                    markerForGeofence(crowdpoint);
                                    startGeofence();




                                }

                                if( geoFenceMarker != null ) {
                                    GeofencingRequest geofenceRequest = createGeofenceRequest( mGeofencesList );
                                    addGeofence( geofenceRequest );
                                } else {
                                    Log.e(TAG, "Geofence marker is null");
                                }

                            } else {



                            }


                        }
                    }

                });

    }


    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d("test", "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius ) {
        Log.d("test", "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                //omeback to this
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    private GeofencingRequest createGeofenceRequest( List<Geofence> geofence ) {
        Log.d("test", "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofences( geofence )
                .build();
    }
    private PendingIntent createGeofencePendingIntent() {
        Log.d("test", "createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d("test", "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }



    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;
    private void drawGeofence() {
        Log.d("test", "drawGeofence()");
        int si = mMarkersList.size();
        if ( geoFenceLimits != null )
            geoFenceLimits.remove();
        for (int nav =0;nav<si;nav++) {
    CircleOptions circleOptions = new CircleOptions()
            .center(mMarkersList.get(nav).getPosition())
            .strokeColor(Color.argb(50, 70, 70, 70))
            .fillColor(Color.argb(100, 150, 150, 150))
            .radius(GEOFENCE_RADIUS).clickable(true);
    geoFenceLimits = mMap.addCircle(circleOptions);
        }

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {


                // add fuction to show fracment uponclicking of this screen
            }
        });
    }

    // Start Geofence creation process
    private void startGeofence() {


        Log.i(TAG, "startGeofence()");
        if( geoFenceMarker != null ) {
          //  Geofence geofence = createGeofence( geoFenceMarker.getPosition(), GEOFENCE_RADIUS );
            mGeofencesList.add(createGeofence( geoFenceMarker.getPosition(), GEOFENCE_RADIUS ));
            //GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
           // addGeofence( geofenceRequest );
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    private Marker geoFenceMarker;
    // Create a marker for the geofence creation
    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( mMap!=null ) {
            // Remove last geoFenceMarker
            if (geoFenceMarker != null)
                geoFenceMarker.remove();

            geoFenceMarker = mMap.addMarker(markerOptions);
            mMarkersList.add(geoFenceMarker);
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
        String currentUserCrowd = ParseUser.getCurrentUser().getString("crowdid");
        decrement(currentUserCrowd);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    @Override
    public void onResult(@NonNull Status status) {

        Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            drawGeofence();
        } else {
            // inform about fail
        }
    }
    static Intent makeNotificationIntent(Context geofenceService, String msg)
    {
        Log.d(TAG,msg);
        return new Intent(geofenceService,MapsActivity.class);
    }


    public void logout(View view) {

        String currentUserCrowd = ParseUser.getCurrentUser().getString("crowdid");
        decrement(currentUserCrowd);
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }


    public void startTransitinActivityRecognition() {
// public void logout(View view) {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent());

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(mContext,
                        getString(R.string.activity_updates_enabled),
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(true);
                updateDetectedActivitiesList();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, getString(R.string.activity_updates_not_enabled));
                Toast.makeText(mContext,
                        getString(R.string.activity_updates_not_enabled),
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(false);
            }
        });
    }

    private PendingIntent getActivityDetectionPendingIntent() {

        Intent intent = new Intent(this, TransitionIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requesting) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, requesting)
                .apply();

    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList() {
         detectedActivities = Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(Constants.KEY_DETECTED_ACTIVITIES, ""));


    }
    public void removeActivityUpdatesButtonHandler(View view) {
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(mContext,
                        getString(R.string.activity_updates_removed),
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(false);
                // Reset the display.

            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Failed to enable activity recognition.");
                Toast.makeText(mContext, getString(R.string.activity_updates_not_removed),
                        Toast.LENGTH_SHORT).show();
                setUpdatesRequestedState(true);
            }
        });
    }

    public void updateDatabase(final Location CurrentUserLocation){


        final String currentUserCrowd = ParseUser.getCurrentUser().getString("crowdid");

      //  final String currentUserCrowd = ParseUser.getCurrentUser().getString("crowdid");

        final ParseGeoPoint GeoPoint = new ParseGeoPoint(CurrentUserLocation.getLatitude(), CurrentUserLocation.getLongitude());
        ParseQuery<ParseObject> query =  ParseQuery.getQuery("Crowds");
        query.whereNear("location",GeoPoint);
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e==null) {

                    if (objects.size() > 0) {

                        for (ParseObject object : objects) {

                          String IDofClosestCrowd= object.getObjectId();
                          ParseGeoPoint objectlocation = (ParseGeoPoint) object.get("location");

                          if (IDofClosestCrowd.equals(currentUserCrowd)){

                              float results[] = new float[]{0,0,0};
                              Location.distanceBetween(objectlocation.getLatitude(),objectlocation.getLongitude(),
                                      CurrentUserLocation.getLatitude(),CurrentUserLocation.getLongitude(),results);

                              //if distance between the first object which is the closest is  greater than  30 then they are a new crowd
                              if (results[0]>30 ) {
                                  decrement(currentUserCrowd);
                              }


                          }else {

                              float results[] = new float[]{0,0,0};
                              Location.distanceBetween(objectlocation.getLatitude(),objectlocation.getLongitude(),
                                      CurrentUserLocation.getLatitude(),CurrentUserLocation.getLongitude(),results);

                              //if distance between the first object which is the closest is  greater than  30 then they are a new crowd
                              if (results[0]<30 ) {
                                  ParseUser.getCurrentUser().put("crowdid",IDofClosestCrowd);
                                  ParseUser.getCurrentUser().put("InOrOut", true);
                                  ParseUser.getCurrentUser().saveInBackground();
                                  object.increment("poeple",1);
                                  object.saveInBackground();

                              }else {
                                  newCrowObject(GeoPoint);
                              }
                               decrement(currentUserCrowd);
                              }
                        }
                    }else {
                        newCrowObject(GeoPoint);

                    }
                }
            }
        });



    }

      public void decrement(String currentUserCrowd){
                ParseQuery<ParseObject> query2 = ParseQuery.getQuery("Crowds");
                query2.whereEqualTo("objectId", currentUserCrowd);
                query2.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() > 0) {

                                for (ParseObject object2 : objects) {
                                    int crowdmag = (int) object2.get("poeple");
                                    object2.increment("poeple", -1);
                                    object2.saveInBackground();
                                    crowdmag--;
                                    if (crowdmag <= 0) {
                                        object2.deleteInBackground();
                                    }
                                    ParseUser.getCurrentUser().put("crowdid", "empty");
                                    ParseUser.getCurrentUser().put("InOrOut", false);
                                    ParseUser.getCurrentUser().saveInBackground();

                                }
                            }


                        }
                    }

                });

            }

            public void newCrowObject(ParseGeoPoint GeoPoint){

                ParseObject request = new ParseObject("Crowds");

//                ParseUser.getCurrentUser().put("crowdid",request.getObjectId());
                ParseUser.getCurrentUser().put("InOrOut", true);
                ParseUser.getCurrentUser().saveInBackground();
                request.put("poeple", 1);
                request.put("location", GeoPoint);
                request.saveInBackground();
                String newObjectID= request.getObjectId();
                ParseUser.getCurrentUser().put("crowdid",newObjectID);
                ParseUser.getCurrentUser().saveInBackground();

            }
            public void updateMarkerList(){


             int size = mMarkersList.size();
             for (int j=0;j<size;j++){


             }
            }


    }





