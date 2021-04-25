package com.example.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private static final String TAG = "DriverMapActivity";

    private FusedLocationProviderClient mFusedLocationClient;
    private SupportMapFragment mapFragment;

    private Button mLogout, mSettings, mHistory, mRideStatus;

    private String customerId="";
    private Boolean isLogginOut= false;

    private LinearLayout mCustomerInfo;

    private ImageView mCustomerProfileImage;

    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;
    private Marker pickupMarker;

    private int status = 0;
    private String destination;
    private LatLng destinationLatLng;

    private LatLng pickupLatLng;

    private Switch mWorkingSwitch;
    private float rideDistance=0;


    private int recordRideWasCalled = 0;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        Log.e(TAG, "ERROR onStart()");
        Log.w(TAG, "WARN onStart()");
        Log.i(TAG, "INFO onStart()");
        Log.d(TAG, "DEBUG onStart()");
        Log.v(TAG, "VERBOSE onStart()");

        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogginOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCustomerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);
        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);


        mSettings = (Button)findViewById(R.id.settings) ;
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogginOut = true;
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mRideStatus = (Button)findViewById(R.id.rideStatus);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              switch (status){
                  case 1:
                      status =2;
                      erasePolylines();

                      if (destinationLatLng!=null){
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0){
                          getRouteToMarker(destinationLatLng);
                        }
                      }
                      mRideStatus.setText("Drive Completed");
                      break;
                  case 2:
                      recordRide();
                      //endRide();
                      break;
              }


            }
        });

        mHistory = (Button) findViewById(R.id.history);
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(DriverMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Drivers");
                startActivity(intent);
                return;
            }
        });

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();

                }


            }
        });

        getAssignedCustomer();

    }


    /**
     * Metoda ce verifica daca soferul logat are asociat un pasager pentru transport
     * In caz afirmativ, are loc preluarea informatiilor pasagerului repsectiv si cursa propriu-zisa
     */

    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    status=1;
                    customerId = dataSnapshot.getValue().toString();
                    notifyDriver();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
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
     * Preluarea informatiilor pasagerului
     */
    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mCustomerName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!=null){
                        mCustomerPhone.setText(map.get("phone").toString());

                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private  DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener asassignedCustomerPickupLocationRefListener;
    /**
     * Preluarea locatiiei pasgerului din baza de date, afisarea lui pe harta si apelarea metodei ce va contrui ruta dintre pasager si sofer
     */
    private void getAssignedCustomerPickupLocation(){
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customersRequest").child(customerId).child("l");
        asassignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>)dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) !=null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) !=null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat,locationLng);
                    pickupMarker = mMap.addMarker((new MarkerOptions().position(pickupLatLng).title("Pickup Location"))); //.icon(bitmapDescriptorFromVector(getApplicationContext(),R.mipmap.ic_customer)));               //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
                    getRouteToMarker(pickupLatLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    /**
     * Metoda responsabila de resetarea varaiabilelor si a bazei de date pentru urmatoarele rulari
     */
    private void endRide(){
        mRideStatus.setText("Picked Customer");
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        //if(finalDestination!=null) {
            customerId = "";
        //}
        rideDistance =0;

        if (pickupMarker!=null){
            pickupMarker.remove();
        }

        if(asassignedCustomerPickupLocationRefListener!=null) {
            assignedCustomerPickupLocationRef.removeEventListener(asassignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: ---");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_customer);

        destinationLatLng=null;
        destination = "undefine by customer";
        finalDestination = null;

    }


    /**
     * Preluarea destinatiei introduse de pasager din baza de date
     */
    private void getAssignedCustomerDestination(){

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();//driverul care e logat
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("destination") !=null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination " + destination);
                    }else{
                        mCustomerDestination.setText("Destination: ----");
                    }
                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Metoda ce scrie in baza de date informatiile privin cursa ce a realizat-o un sofer, atat in sectiunea history, unde sunt toate cursele efectuate
     * de catre soferi, cat si in sectiunea soferului si a pasagerului ce au realizat respectiva cerere de transport
     */
    private String requestId;
    private void recordRide(){
        recordRideWasCalled=1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        requestId = historyRef.push().getKey();  //creem chei unice
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);

        if(destinationLatLng!=null) {
            if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                map.put("location/to/lat", destinationLatLng.latitude);
                map.put("location/to/lng", destinationLatLng.longitude);
            } else {
                map.put("location/to/lat", mLastLocation.getLatitude());
                map.put("location/to/lng", mLastLocation.getLongitude());
            }
        } else {
            map.put("location/to/lat", mLastLocation.getLatitude());
            map.put("location/to/lng", mLastLocation.getLongitude());
        }


        if(!customerId.equals("") && finalDestination!=null) {
            map.put("distance", rideDistance/1000);
            historyRef.child(requestId).updateChildren(map);
            endRide();
            recordRideWasCalled=0;
        }
        else {
            erasePolylines();
            getRouteToMarker(pickupLatLng);
        }


    }

    /**
     * Introducerea informatiilor in baza de date privind cursa cand pasagerul nu selecteaza o destinatie
     */
    private void recordRideWhenCustomerDontSelectDestination(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        HashMap map = new HashMap();
        map.put("distance", rideDistance/1000);
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);

        if(destinationLatLng!=null) {
            if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                map.put("location/to/lat", destinationLatLng.latitude);
                map.put("location/to/lng", destinationLatLng.longitude);
            } else {
                map.put("location/to/lat", mLastLocation.getLatitude());
                map.put("location/to/lng", mLastLocation.getLongitude());
            }
        } else {
            map.put("location/to/lat", mLastLocation.getLatitude());
            map.put("location/to/lng", mLastLocation.getLongitude());
        }
        historyRef.child(requestId).updateChildren(map);
        recordRideWasCalled=0;
        erasePolylines();
        customerId="";
        endRide();
    }




    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    /**
     * callback repsponsabil de actualizarea locatiilor pe harta, calcularea distantelor dintre pasager si destinatie cat si actualizarea acestora in
     * baza de date
     */
    private Location finalDestination=null;
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){

                    if(destinationLatLng!=null) {
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            finalDestination = new Location(LocationManager.GPS_PROVIDER);
                            finalDestination.setLatitude(destinationLatLng.latitude);
                            finalDestination.setLongitude(destinationLatLng.longitude);
                        }
                    }


                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13.0f));
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);


                    switch(customerId){
                        case "":
                            geoFireWorking.removeLocation(userId);
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                        default:
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                    }


                }
            }
        }
    };

    /**
     * Crearea cererii pentru acordarea permisiunii de a accesa locatia utilizatorului
     */
    private void checkLocationPermision() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Give permission to acces your location to be able to run the app")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
                            }
                        })
                        .create()
                        .show();

            }else{
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
        }
    }

    /**
     * Prelucrarea permisiunii oferite de catre utilizator (daca acesta a oferit sau nu permisiunea)
     */
    final int LOCATION_REQUEST_CODE =1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    /**
     * Conectarea soferului, se verifica permisiunilepentru locatie. Daca acesta le-a oferit se trece la actualizarea locatiilor acestuia
     * pe harta, daca nu, ii este ceruta acceptarea permisiunilor necesare
     */
    private void connectDriver() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){   //Build.VERSION_CODES.M
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                checkLocationPermision();
            }
        }

    }


    /**
     * metoda apelata cand se deconecteaza soferul
     */
    private void disconnectDriver(){
        if(mFusedLocationClient!=null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

    }


    @Override
    protected void onResume() {
        super.onResume();
        isLogginOut=false;
    }

    /**
     * Metoda responsabila de crearea rutelor
     */
    private void getRouteToMarker(LatLng pickupLatLng){

        Routing routing = new Routing.Builder()
                .key(getString(R.string.google_maps_key))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }


    /**
     * Afisarea rutelor pe interfata dispozitivelor (harta aplciatiei)
     */
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }
        rideDistance = 0;

        polylines = new ArrayList<>();

        for (int i = 0; i <route.size(); i++) {
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            rideDistance = route.get(i).getDistanceValue();
            if(recordRideWasCalled==1){
                recordRideWhenCustomerDontSelectDestination();
            }
            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    /**
     * stergerea rutelor de pe harta
     */
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLogginOut==false) {
            disconnectDriver();
        }
    }

    /**
     * Metoda responsabila de afisarea unui mesaj in care utilizatorul este intrebat daca chiar doreste a parasi aplicatia cand acesta apasa
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DriverMapActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Notificarea soferilor in momentul in care acestia au asociati pasageri pentru ai transporta
     */
    public void notifyDriver(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID");

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_car)
                .setTicker("Hearty365")
                .setPriority(Notification.PRIORITY_MAX) // this is deprecated in API 26 but you can still use for below 26. check below update for 26 API
                .setContentTitle("Notification by Uber")
                .setContentText("You have a request from a customer")
                .setContentInfo("Info");

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notificationBuilder.build());
    }

}
