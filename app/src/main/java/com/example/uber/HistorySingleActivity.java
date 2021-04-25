package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.internal.ViewUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.Long.valueOf;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private String rideId, currentUserId, customerId, driverId, userDriverOrCustomer;
    private TextView locationRide;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView userName;
    private TextView userPhone;
    private TextView customerPaidTheRide;
    private TextView priceForRide;

    private ImageView userImage;
    private DatabaseReference historyRideInfoDb;

    private LatLng destinationLatLng, pickupLatLng;
    private static final String TAG = "HistorySingleActivity";

    private RatingBar mRatingBar;
    private String distance;
    private String ridePrice;
    private Button mPay;
    private Boolean customerPaid = false;




    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        //paypal
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        polylines = new ArrayList<>();
        rideId = getIntent().getExtras().getString("rideId");
        rideDistance = (TextView)findViewById(R.id.rideDistance);
        rideDate = (TextView)findViewById(R.id.rideDate);
        userName = (TextView)findViewById(R.id.userName);
        userPhone = (TextView)findViewById(R.id.userPhone);
        customerPaidTheRide=findViewById(R.id.custPaid);
        priceForRide=findViewById(R.id.price);

        mRatingBar = (RatingBar)findViewById(R.id.ratingBar);

        userImage = (ImageView)findViewById(R.id.userImage);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        locationRide = (TextView)findViewById(R.id.rideLocation);

        mPay = findViewById(R.id.pay);

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();


    }

    /**
     * incarcarea informatiilor din baza de date, pe interfata aplicatiei, in momentul in care utilizatorul da click pe o cursa din istoric
     */
    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot child : dataSnapshot.getChildren()){
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if (!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUserInformation("Customers", customerId);
                            }
                        }
                        if (child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if (!driverId.equals(currentUserId)){
                                userDriverOrCustomer = "Customers";
                                getUserInformation("Drivers", driverId);
                                displayCustomerRelatedObjects();

                            }
                        }
                        if (child.getKey().equals("timestamp")){
                            rideDate.setText("Date: "+ getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("rating")){
                          mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }

                        if (child.getKey().equals("customerPaid")) {
                            customerPaid = true;
                            customerPaidTheRide.setText("Customer paid: "+ child.getValue().toString());
                        }

                        if (child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            rideDistance.setText("Distance: "+distance.substring(0, Math.min(distance.length(),5)) + " km");
                           // ridePrice = Double.valueOf(distance)*3;
                        }
                        if (child.getKey().equals("price")){
                            ridePrice = child.getValue().toString();
                            priceForRide.setText("Price: "+String.valueOf(ridePrice).substring(0, Math.min(String.valueOf(ridePrice).length(),5)) + " USD");
                        }

                        if (child.getKey().equals("destination")){
                            locationRide.setText("Destination: "+ child.getValue().toString());
                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            if(child.child("to").child("lat").getValue()!=null)
                            {
                                destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            }
                            if (destinationLatLng != new LatLng(0,0) && destinationLatLng!=null){
                                getRouteToMarker();
                            }

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Metoda ce afiseaza anumite view-uri, in functie de tipul utilizatorului.
     * In cazul in care utilizatorul este pasager, pe interfata acestuia va fi incarcat si informatii precum rating cat si functia de plata.
     */
    private void displayCustomerRelatedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mPay.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(rating);

            }
        });
        if (customerPaid){
            mPay.setEnabled(false);   //daca customerul a platiti(customerPaid=true), dezactivam butonul, ca sa nu plateasca de mai multe ori din greaseala
        }
        else{
            mPay.setEnabled(true);
        }
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paypalPayment();
            }
        });

    }

    /**
     * configurarea paypal
     */
    private int PAYPAL_REQUEST_CODE= 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);

    /**
     *metoda ce efectueaza plata pasagerului
     */
    private void paypalPayment() {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(ridePrice), "USD", "Uber Ride",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);

    }

    /**
     * Metoda ce se ocupa de rezultatul primit de catre plata paasgerului
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if(confirm!=null){
                    try {
                        JSONObject jsonObject = new JSONObject(confirm.toJSONObject().toString());
                        String paymentResponse = jsonObject.getJSONObject("response").getString("state");
                        if (paymentResponse.equals("approved")){
                            Toast.makeText(getApplicationContext(), "Payment Successful!", Toast.LENGTH_LONG).show();
                            historyRideInfoDb.child("customerPaid").setValue(true);
                            mPay.setEnabled(false);
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }

            }else{
                Toast.makeText(getApplicationContext(), "Payment unsuccessful!", Toast.LENGTH_LONG).show();
            }
        }


    }

    /**
     * Incarcarea informatiilor utilizatorilor in istoric
     */
    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") !=null){
                        userName.setText("Name: " + map.get("name").toString());
                    }
                    if (map.get("phone") !=null){
                        userPhone.setText("Phone: " + map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") !=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /**
     * Transformarea timestamp in data calendaristica
     */
    private String getDate(Long timestamp){

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();

        return date;
    }


    /**
     * incarcarea hartii pe dispozitiv
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap =googleMap;
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * crearea rutelor
     */
    private void getRouteToMarker(){

        Routing routing = new Routing.Builder()
                .key(getString(R.string.google_maps_key))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingStart() {
    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    /**
     * Afisarea rutelor pe harta
     */
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location")); //.icon(Bitmap.....
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination")); //.icon(Bitmap.....


        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();

        for (int i = 0; i <route.size(); i++) {


            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

           // Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    /**
     * Curatarea variabilei polyline ce contine rutele, pentru urmatoarele utilizari
     */
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }

    @Override
    protected void onDestroy(){
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }


}


