package pt.ipleiria.helprecycle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Maps;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;

import pt.ipleiria.helprecycle.ARCode.SelectedOneLabelArActivity;
import pt.ipleiria.helprecycle.Maps.ClusterManagerRenderer;
import pt.ipleiria.helprecycle.Maps.ClusterMarker;
import pt.ipleiria.helprecycle.Maps.RecycleBin;
import pt.ipleiria.helprecycle.common.PolylineData;
import pt.ipleiria.helprecycle.common.Singleton;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private GoogleMap mMap;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int MANUAL_PERMISSION_REQUEST_CODE = 4444;
    private static final int EXIT_GPS_ACTIVATION_CODE = 5555;

    private Boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ClusterManager mClusterManager;
    private ClusterManagerRenderer mClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    private LatLngBounds mMapBoundary;
    private GeoApiContext mGeoApiContext = null;
    private ArrayList<PolylineData> mPolylineData = new ArrayList<>();
    private Marker mSelectedMarker = null;
    private ArrayList<Marker> mDestinationMarkers = new ArrayList<>();

    private Location currentLocation;
    private LocationManager locationManager;
    private ArrayList<RecycleBin> recycleBins = new ArrayList<>();


    private boolean dialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if(mGeoApiContext == null){
            //Necessario quem tiver a API Key atual adicionar a "Directions API.
            //Por enquanto, vou inserir a minha manualmente

            //mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_api_key)).build();
            mGeoApiContext = new GeoApiContext.Builder().apiKey("AIzaSyBa783nQt2X0V_-eDPn6Bh-a5wJpGxWr1Y").build();
        }
        populateRecycleBins();

        getLocationPermission();

        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnPolylineClickListener(this);

        //Testando com limites de Zoom
        mMap.setMinZoomPreference(16f);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        addMapMarkers();
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                boolean inRange;
                resetSelectedMarker();
                mSelectedMarker = marker;

                Location markerLocation = new Location("MarkerLocation");
                markerLocation.setLatitude(marker.getPosition().latitude);
                markerLocation.setLongitude(marker.getPosition().longitude);

                getDeviceLocation();

                float distance = currentLocation.distanceTo(markerLocation);

                System.out.println("distance is" + distance);
                if(distance <= 20){
                    inRange = true;
                }else{
                    inRange = false;
                }

                binInteractionMessage(inRange, marker);
            }
        });

        verifyAndGetGPS();
    }

    private void removeUnusedMarkers(){
        for(Marker m: mDestinationMarkers){
            m.remove();
        }
    }

    private void resetSelectedMarker(){
        if(mSelectedMarker != null){
            mSelectedMarker.setVisible(true);
            mSelectedMarker = null;
            removeUnusedMarkers();
        }
    }

    //If the user is in range, It will let the user play!
    //otherwise, it will tell the user to get closer
    private void binInteractionMessage(boolean i, Marker marker){
        if(dialogShown){
            //Do nothing pls
        }else{
            dialogShown = true;
            final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            if(i){
                builder.setMessage("You're in range. What do you wish to do?")
                        .setCancelable(true)
                        .setPositiveButton("Analyze!", (dialog, which) -> {
                            dialogShown = false;
                            dialog.dismiss();
                            dismiss();
                        })
                        .setNegativeButton("I've already Analyzed", (dialog, which) -> {
                            dialogShown = false;
                            dialog.cancel();
                            dialog.dismiss();
                            if(Singleton.getInstance().getAnswer().equals("NOTHING")){
                                final AlertDialog.Builder anotherBuilding = new AlertDialog.Builder(MapsActivity.this);
                                anotherBuilding.setMessage("You have nothing Analyzed")
                                        .setCancelable(true)
                                        .setPositiveButton("Understood", (dialog1, which1) -> {
                                            dialogShown = false;
                                            dialog.dismiss();
                                        });
                                final AlertDialog alert2 = anotherBuilding.create();
                                alert2.show();
                            }else{
                                Intent intentPrevious = new Intent(this, MainActivity.class);
                                startActivity(intentPrevious);
                                Intent intentCurrent = new Intent(this, SelectedOneLabelArActivity.class);
                                startActivity(intentCurrent);
                                this.finish();
                            }
                        });
            }else{
                builder.setMessage("You're too far from this recycling station. Get closer!")
                        .setCancelable(true)
                        .setPositiveButton("Get directions", (dialog, which) -> {
                            calculateDirections(marker);
                            dialogShown = false;
                            dialog.dismiss();
                        })
                        .setNegativeButton("Close", (dialog, which) -> {
                            dialogShown = false;
                            dialog.dismiss();
                        });
            }
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void dismiss(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }

    private void drawCircle(LatLng point){

        // Instantiating CircleOptions to draw a circle around the marker
        CircleOptions circleOptions = new CircleOptions();

        // Specifying the center of the circle
        circleOptions.center(point);

        // Radius of the circle
        circleOptions.radius(20);

        // Border color of the circle
        circleOptions.strokeColor(Color.BLACK);

        // Fill color of the circle
        circleOptions.fillColor(0x30ff0000);

        // Border width of the circle
        circleOptions.strokeWidth(2);

        // Adding the circle to the GoogleMap
        mMap.addCircle(circleOptions);

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

    //Makes and adds the custom markers to the map
    //Need to make a marker list, so that I can spread them out
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
            for(RecycleBin r: recycleBins){

                //System.out.println("addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try{
                    int avatar = R.drawable.recycle; // set the default avatar

                    ClusterMarker newClusterMarker = new ClusterMarker(
                            r.getLocation(),
                            r.getTitle(),
                            r.getSnippet(),
                            avatar
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);
                    drawCircle(newClusterMarker.getPosition());


                }catch (NullPointerException e){
                    System.out.println("addMapMarkers: NullPointerException: " + e.getMessage() );
                }

            }
            mClusterManager.cluster();

            //setCameraView();
        }
    }

    private void populateRecycleBins(){
        //Valor obtido através do website: http://mapasonline.cm-leiria.pt/MuniSIGInter/Html5Viewer/index.html?viewer=Gesto_de_Resduos_Urbanos_e_Higiene_Pblica.Gesto_Resduos_Urbanos_e_Higiene_Pblica&fbclid=IwAR38eDZqi04ICOM8UTimqg8AAs9PvHayejVr9l_FFITE5UlbyN9qduGe5XM
        //ESTG
        recycleBins.add(new RecycleBin("ESTG Automóvel", new LatLng(39.73313, -8.82109)));
        recycleBins.add(new RecycleBin("ESTG Cantina 3", new LatLng(39.734689, -8.822158)));
        recycleBins.add(new RecycleBin("ESTG Edificio A", new LatLng(39.735039, -8.820771)));

                //My House 2
                //new LatLng(39.75607, -8.77968),
        recycleBins.add(new RecycleBin("Rodrigo House #1",new LatLng(39.75607, -8.77968)));

                //My House
                //new LatLng(39.756517, -8.779753),))
        recycleBins.add(new RecycleBin("Rodrigo House #2",new LatLng(39.756517, -8.779753)));
    }

    private void verifyAndGetGPS() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            getAndSetLocation();
        }else{
            GPSDisabled();
        }
    }

    private void getAndSetLocation (){
        getDeviceLocation();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setCameraView(currentLocation);
            }
        }, 1000);   //5 seconds

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
        if(dialogShown){
            //Do nothing pls
        }else{
            dialogShown = true;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialog);
            alertDialogBuilder.setMessage("This app needs access to your GPS and it is disabled. Would you like to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Enable GPS",
                            new DialogInterface.OnClickListener(){
                                public void onClick(DialogInterface dialog, int id){
                                    dialogShown = false;
                                    Intent GPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(GPSSettingIntent, EXIT_GPS_ACTIVATION_CODE);
                                }
                            });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }
    }

    private void getDeviceLocation(){
        try{
            if(mLocationPermissionGranted){
                mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            Location location = task.getResult();
                            currentLocation = location;
                            //setCameraView(location);
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

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void calculateDirections(Marker marker){

        //Determine destination
        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        //Determina mais do que uma rota
        //directions.alternatives(true);
        directions.alternatives(false);

        //Determine origin
        directions.origin(
                new com.google.maps.model.LatLng(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude()
                )
        );
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                System.out.println("calculateDirections: routes: " + result.routes[0].toString());
                System.out.println("calculateDirections: duration: " + result.routes[0].legs[0].duration);
                System.out.println("calculateDirections: distance: " + result.routes[0].legs[0].distance);
                System.out.println("calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                //ACTUAL METHOD, LIMITED API CALLS
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                System.out.println("calculateDirections: Failed to get directions: " + e.getMessage());
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(() -> {
            System.out.println("run: result routes: " + result.routes.length);
            if(mPolylineData.size() > 0){
                for(PolylineData p: mPolylineData){
                    p.getPolyline().remove();
                }
                mPolylineData.clear();
                mPolylineData = new ArrayList<>();
            }

            //ACTUAL METHOD
            for(DirectionsRoute route: result.routes){
                System.out.println("run: leg: " + route.legs[0].toString());
                List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                List<LatLng> newDecodedPath = new ArrayList<>();

                for(com.google.maps.model.LatLng latLng: decodedPath){
                    newDecodedPath.add(new LatLng(
                            latLng.lat,
                            latLng.lng
                    ));
                }

                Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));

                polyline.setColor(ContextCompat.getColor(MapsActivity.this, R.color.Blue));
                polyline.setClickable(true);
                mPolylineData.add(new PolylineData(polyline, route.legs[0]));

                onPolylineClick(polyline);
                zoomRoute(polyline.getPoints());

                mSelectedMarker.setVisible(false);
            }
        });

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
        if(dialogShown){
            //Do nothing pls
        }else{
            dialogShown = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
            builder.setMessage("This app needs permission to access your location. Please set it manually.");
            builder.setCancelable(false);
            builder.setPositiveButton("Permit Manually", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialogShown = false;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.app_bar_nearest:
                //getClosest
                Toast.makeText(MapsActivity.this, "Retrieving nearest recycling bin", Toast.LENGTH_SHORT).show();

                getDeviceLocation();

                //random high value
                float distance = -1;
                //Location bestLocation = new Location("RecycleLocation1");
                RecycleBin bestBin = new RecycleBin("",null);
                for (RecycleBin recycleBin : recycleBins) {
                    if(distance == -1){
                        //first time, we dont have a distance yet
                        Location recycleLocation = new Location("RecycleLocation");
                        recycleLocation.setLatitude(recycleBin.getLocation().latitude);
                        recycleLocation.setLongitude(recycleBin.getLocation().longitude);

                        distance = currentLocation.distanceTo(recycleLocation);
                        bestBin = recycleBin;
                    }else{
                        Location recycleLocation = new Location("RecycleLocation");
                        recycleLocation.setLatitude(recycleBin.getLocation().latitude);
                        recycleLocation.setLongitude(recycleBin.getLocation().longitude);

                        float newDistance = currentLocation.distanceTo(recycleLocation);

                        if(newDistance < distance){

                            bestBin = recycleBin;
                        }
                    }
                }


                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bestBin.getLocation(), 18.5f));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //HAS A GLITCH, IF YOU SPAM THE LOCATION BUTTON THE POPUL WILL SPAWN FOREVER
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                    // Make an action or refresh an already managed state.
                    Toast.makeText(MapsActivity.this, "Trying to detect GPS signal...", Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        verifyAndGetGPS();
                    }
                }, 4000);   //5 seconds
            }
        }
    };

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        for(PolylineData polylineData: mPolylineData){
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(MapsActivity.this, R.color.Blue));
                polylineData.getPolyline().setZIndex(1);

                LatLng destinationMarker = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(destinationMarker)
                        .title("Destination")
                        .snippet("Duration; " + polylineData.getLeg().duration)
                );

                marker.showInfoWindow();

                mDestinationMarkers.add(marker);
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(MapsActivity.this, R.color.Gray));
                polylineData.getPolyline().setZIndex(0);
            }
        }
    }
}
