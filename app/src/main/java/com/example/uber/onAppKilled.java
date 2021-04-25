package com.example.uber;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class onAppKilled extends Service {
    private static final String TAG = "onAppKilled";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * metoda ce este apelata cand aplicatia este distrusa din memorie.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);


        DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFire2 = new GeoFire(ref2);
        geoFire2.removeLocation(userId);
    }
}
