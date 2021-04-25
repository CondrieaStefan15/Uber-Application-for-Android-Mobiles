package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;
    private SupportMapFragment mapFragment;

    private Button mLogout, mRequest, mSettings, mHistory;

    private LatLng pickupLocation;

    private Boolean requestBol = false;
    private Marker pickupMarker;
    private String customerId="";

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverPhone, mDriverCar, mLicensePlate;
    private float rideDistance;
    private String destination, requestService;

    private RadioGroup mRadioGroup;
    private PlacesClient placesClient;
    private int AUTOCOMPLETE_REQUEST_CODE;
    private static final String TAG = "CustomerMapActivity";

    private LatLng destinationLatLng;

    private RatingBar mRatingBar;

    private boolean isAplicationOnBackground;


    /**
     * Metoda ce se apeleaza ori de cate ori o activitate este lansata si in care are loc crearea elementelor de pe interfata
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mLicensePlate = findViewById(R.id.licensePlate);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.UberX);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);



        destinationLatLng = new LatLng(0.0,0.0);

        isAplicationOnBackground=false;



        mSettings = (Button)findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });




        /** PLACE AUTOCOMPLETE
         * Initialize Places. For simplicity, the API key is hard-coded. In a production
         * environment we recommend using a secure mechanism to manage API keys.
         */
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });




        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mHistory = (Button) findViewById(R.id.history);
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });



        mRequest = (Button)findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestBol){
                    endRide();
                }else{
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton)findViewById(selectId);

                    if(radioButton.getText() == null){
                        return;
                    }

                    requestService = radioButton.getText().toString();

                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customersRequest");
                    GeoFire geoFire = new GeoFire(ref);

                    if(mLastLocation!=null) {
                        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                        pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));           //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
                    }
                    mRequest.setText("Getting your driver...");

                    getClosestDriver();
                }
            }
        });
    }




    private int radius =1;
    private Boolean driverFound = false;
    private String driverFoundId;

    /**
     * Metoda responsabila de asocierea cererilor de trasnport, a unor soferi disponibili in acel moment si care indeplinesc cerintele cererii
     */
    GeoQuery geoQuery;
    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);

        if(pickupLocation!=null) {
            geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
            geoQuery.removeAllListeners();
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (!driverFound && requestBol) {
                        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                    Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                    if (driverFound) {
                                        return;
                                    }

                                    if (driverMap.get("service").equals(requestService)) {
                                        driverFound = true;
                                        driverFoundId = dataSnapshot.getKey();

                                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerId);
                                        if (destinationLatLng != null) {
                                            map.put("destination", destination);
                                            map.put("destinationLat", destinationLatLng.latitude);
                                            map.put("destinationLng", destinationLatLng.longitude);
                                        }
                                        driverRef.updateChildren(map);
                                        if (isAplicationOnBackground) {
                                            notifyCustomer();
                                        }
                                        getDriverLocation();
                                        getDriverInfo();
                                        theRideisOver();
                                        mRequest.setText("Looking for Driver location..");
                                    }

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if (!driverFound && radius < 20) {
                        radius++;
                        getClosestDriver();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }
    }


    private DatabaseReference driveHasEndedref;
    private ValueEventListener driveHasEndedRefListener;
    /**
     * Metoda responsabila de determinarea momentului cand se apeleaza metoda endRide(), responsabila de resetarea tuturor variabilelor
     * si pregatirea lor pentru o alta rulare.
     */
    private void theRideisOver(){
        driveHasEndedref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                }else{
                    endRide();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Metoda responsabila de resetarea variabilelor si a anumitor date din baza de date, dupa ce o cerere de transport a fost efectuata
     */
    private void endRide(){
        requestBol = false;
        if(geoQuery != null) {
            geoQuery.removeAllListeners();
        }
        if (driverLocationRef != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
        }
        if (driveHasEndedref!= null) {
            driveHasEndedref.removeEventListener(driveHasEndedRefListener);
        }
        if (driverFound==true){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            driverRef.removeValue();
            driverFoundId= null;
        }
        else
        { }

        driverFound= false;
        radius =1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
        if (pickupMarker!=null){
            pickupMarker.remove();
        }
        mRequest.setEnabled(true);
        mRequest.setText("Call Uber");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_customer);

        destination=null;
        destinationLatLng=null;
        isAplicationOnBackground =false;



    }


    /**
     * Metoda folosita pentru extragerea datelor soferului asociat unei cereri de transport, date ce sunt afisate pe interfata aplicatiei
     */
    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mDriverName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!=null){
                        mDriverPhone.setText(map.get("phone").toString());

                    }
                    if(map.get("car")!=null){
                        mDriverCar.setText(map.get("car").toString());

                    }
                    if(map.get("licensePlate")!=null){
                        mLicensePlate.setText(map.get("licensePlate").toString());

                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingAvg = 0;

                    for(DataSnapshot child: dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal ++;
                    }

                    if(ratingsTotal!=0){
                        ratingAvg = ratingSum/ratingsTotal;

                        mRatingBar.setRating(ratingAvg);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    /**
     * Metoda responsabila de extragerea coordonatelor geografice ale soferului asociat unei cereri de transport si afisarea acestuia pe harta interfetei
     * aplicatiei.
     */
    private void getDriverLocation(){
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver Found");
                    mRequest.setEnabled(false);
                    if(map.get(0) !=null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) !=null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    //notice when driver ARRIVES
                    if(distance<100){
                        mRequest.setText("Driver's Here");
                    }else{
                        mRequest.setText("Driver Found: " + distance);
                    }
                    mDriverMarker = mMap.addMarker((new MarkerOptions().position(driverLatLng).title("Your Driver")));      //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Metoda ce va incarca pe interfata dispozitivului, harta oferita de GoogleMaps.
     * Aici se va cere si permisiunile pentru afisarea locatiei utilizatorului
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){   //Build.VERSION_CODES.M
            if(ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                checkLocationPermision();
            }
        }
    }

    /**
     * Metoda ce va actualiza locatiile pe harta aplicatiei, locatii ce sunt generate de metoda „requestLocationUpdates()” al obiectului
     * de tip FusedLocationClient
     */
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){

                    if (!customerId.equals("") && mLastLocation!=null && location!=null){
                        rideDistance +=mLastLocation.distanceTo(location)/1000;
                    }
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

                    if (!getDriversAroundStarted) {
                        getDriversAround();
                    }


                }
            }
        }
    };

    /**
     * Cererea ce este inaintata utilizatorilor prin care le este cerut acordul de a le accesa locatia si a o folosi in afisarea pe harta a acestuia
     */
    private void checkLocationPermision() {
        if(ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("We need to access your location to run the app")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[] {ACCESS_FINE_LOCATION},1);
                            }
                        })
                        .create()
                        .show();

            }else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[] {ACCESS_FINE_LOCATION},1);
            }
        }
    }

    /**
     * raspunsul ce il ofera utilizatorul la cererea de ai accesa locatia este prelucrata de aceasta metoda.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch(requestCode){
            case 1:{
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Please provide the permission", Toast.LENGTH_LONG);
                }
                break;
            }
        }
    }


    Boolean getDriversAroundStarted =false;
    List<Marker>  markerList = new ArrayList<Marker>();

    /**
     *  Metoda ce afiseaza toti soferii disponibili de pe o anumita raza
     */
    private void getDriversAround(){
        getDriversAroundStarted=true;
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire= new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 100);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {//onKeyEntered e apelat cand se ruleaza pentru prima data addGeoquerryevent listerul si extrage din baza de date toti driverii disponibili

                    LatLng driverLocation = new LatLng(location.latitude, location.longitude);
                    Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key));
                    mDriverMarker.setTag(key);

                    markerList.add(mDriverMarker);
            }

            /**
             *
             * Cand un driver nu mai este disponibil
             */
            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markerList){
                    if (markerIt.getTag().equals(key)){
                        markerIt.remove();
                        markerList.remove(markerIt);
                        return;
                    }
                }
            }

            /**
             * Apelata cand are loc miscarea soferilor
             */
            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markerList){
                    if (markerIt.getTag().equals(key)){
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }


            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAplicationOnBackground = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        isAplicationOnBackground = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(requestBol){
            endRide();
        }
    }

    /**
     *  Metoda responsabila de trimiterea unei notificari in momentul in care pasagerul are aplicatia in background
     */
    public void notifyCustomer(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID");

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_car)
                .setTicker("Hearty365")
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Notification by Uber")
                .setContentText("We find a driver for your request")
                .setContentInfo("Info");

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notificationBuilder.build());
    }

    /**
     *  Metoda apelata cand pasagerul apasa putonul "back" al telefonului, buton responsabil de iesirea si inchiderea aplicatiei.
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CustomerMapActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();


    }

}

