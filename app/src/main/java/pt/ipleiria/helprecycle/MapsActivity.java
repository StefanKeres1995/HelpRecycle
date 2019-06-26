package pt.ipleiria.helprecycle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;

import pt.ipleiria.helprecycle.Maps.ClusterManagerRenderer;
import pt.ipleiria.helprecycle.Maps.ClusterMarker;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private static final int MANUAL_PERMISSION_REQUEST_CODE = 4444;
    private static final int EXIT_GPS_ACTIVATION_CODE = 5555;

    private Boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private ClusterManager mClusterManager;
    private ClusterManagerRenderer mClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();

    private LatLngBounds mMapBoundary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Valor obtido através do website: http://mapasonline.cm-leiria.pt/MuniSIGInter/Html5Viewer/index.html?viewer=Gesto_de_Resduos_Urbanos_e_Higiene_Pblica.Gesto_Resduos_Urbanos_e_Higiene_Pblica&fbclid=IwAR38eDZqi04ICOM8UTimqg8AAs9PvHayejVr9l_FFITE5UlbyN9qduGe5XM
        addMapMarkers();

        if (mLocationPermissionGranted) {
            verifyAndGetGPS();

        }else{
            //do i really need this verification?
        }
    }

    private void setCameraView(Location l) {

        // Set a boundary to start
        double bottomBoundary = l.getLatitude() - .1;
        double leftBoundary = l.getLongitude() - .1;
        double topBoundary = l.getLatitude() + .1;
        double rightBoundary = l.getLongitude() + .1;

        mMapBoundary = new LatLngBounds(
                new LatLng(bottomBoundary, leftBoundary),
                new LatLng(topBoundary, rightBoundary)
        );

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, 0));
    }

    private void addMapMarkers(){

        if(mMap != null){

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<ClusterMarker>(this.getApplicationContext(), mMap);
            }
            if(mClusterManagerRenderer == null){
                mClusterManagerRenderer = new ClusterManagerRenderer(
                        this,
                        mMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }

            //For each recycling bin
            //for(UserLocation userLocation: mUserLocations){

                //System.out.println("addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try{
                    String snippet = "";

                    snippet = "Determine route to Ecoponto #1?";

                    int avatar = R.drawable.recycle; // set the default avatar

                    ClusterMarker newClusterMarker = new ClusterMarker(
                            //new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude()),
                            new LatLng(39.73313, -8.82109),
                            //userLocation.getUser().getUsername(),
                            "ecoponto #1",
                            snippet,
                            avatar
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

                }catch (NullPointerException e){
                    System.out.println("addMapMarkers: NullPointerException: " + e.getMessage() );
                }

            //}
            mClusterManager.cluster();

            //setCameraView();
        }
    }

    private void verifyAndGetGPS() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            getAndSetLocation();
        }else{
            GPSDisabled();
        }
    }

    private void getAndSetLocation (){
        getDeviceLocation();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //this show the user location ball on the map
        mMap.setMyLocationEnabled(true);
    }

    private void GPSDisabled() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialog);
        alertDialogBuilder.setMessage("This app needs access to your GPS and it is disabled. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent GPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(GPSSettingIntent, EXIT_GPS_ACTIVATION_CODE);
                            }
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            //we know current location
                            Location currentLocation = (Location) task.getResult();
                            setCameraView(currentLocation);
                            //moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
                        }else{
                            //we dont know the current location
                            Toast.makeText(MapsActivity.this, "Cant Find current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }catch (SecurityException e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        mLocationPermissionGranted =false;

        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            //User didn't grant location permissions
                            //this function doesnt prevent the user from clicking "Dont show again"--userDidntGrantPermissionHandler();
                            userChoseToNeverAskForPermissions();
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    //verifyAndGetGPS();
                    initMap();
                }
            }
        }
    }

    private void userChoseToNeverAskForPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage("This app needs permission to access your location. Please set it manually.");
        builder.setCancelable(false);
        builder.setPositiveButton("Permit Manually", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, MANUAL_PERMISSION_REQUEST_CODE);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == MANUAL_PERMISSION_REQUEST_CODE){
            getLocationPermission();
        }else if(requestCode == EXIT_GPS_ACTIVATION_CODE){
            //This handler is necessary because we need a time error margin between when the user enables his location and when we verify if it is active.
            //Conclusions:
            //If theres no waiting time: The GPS request will pop up even when the user already enabled his location
            //If the waiting time is too short: It will crash the app because it detected the active GPS but it doesn't have a location established yet
            Toast.makeText(this, "Verifying GPS...", Toast.LENGTH_LONG).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    verifyAndGetGPS();
                }
            }, 4000);   //5 seconds
        }
    }
}
