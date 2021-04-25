package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.example.uber.historyrecyclerView.HistoryAdapter;
import com.example.uber.historyrecyclerView.HistoryObject;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HistoryActivity extends AppCompatActivity {

    private String customerOrDriver, userId;

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private TextView mBalance;
    private Double Balance = 0.0;
    private Button mPayout;
    private EditText mPayoutEmail;
    private static final String TAG = "HistoryActivity";

    /**
     * metoda apelata in momentul in care o activitate este incarcata pe interfata dispozitivului
     * contine referinte catre elementele XML si initializarea unor variabile folostite pe parcursul rularii
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mBalance = findViewById(R.id.balance);
        mPayout = findViewById(R.id.payout);
        mPayoutEmail = findViewById(R.id.payoutEmail);

        mHistoryRecyclerView = findViewById(R.id.historyRecycleView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        
        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();
        if (customerOrDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE);
            mPayout.setVisibility(View.VISIBLE);
            mPayoutEmail.setVisibility(View.VISIBLE);
        }
        
        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payoutRequest();
            }
        });


    }


    /**
     * metoda ce verifica daca in sectiunea aferenta soferului sau pasagerului exista elemente de afisat din seciunea history
     */
    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot history : dataSnapshot.getChildren()){
                        if (history.getValue().toString().equals("true")){
                             FetchRideInformation(history.getKey());
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
     * Afisarea inregistrarilor ce reprezinta cursele efectuate atat de sofer cat si de pasager
     */
    private void FetchRideInformation(String rideKey){
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Double ridePrice = 0.0;
                    Long timestamp = 0L;
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if (child.getKey().equals("timestamp")){
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                    }
//
                    if (dataSnapshot.child("customerPaid").getValue() != null && dataSnapshot.child("driverPaidOut").getValue() == null){
                        if (dataSnapshot.child("price").getValue() !=null){
                            ridePrice = Double.valueOf(dataSnapshot.child("price").getValue().toString());
                            Balance +=ridePrice;
                            //mBalance.setText("Balance: " + String.valueOf(Balance) + "  USD");
                            mBalance.setText("Balance: " + String.format("%.3f", Balance) + "  USD");
                        }
                    }
                    HistoryObject obj = new HistoryObject(rideId, getDate(timestamp));
                    resultsHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private ArrayList resultsHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultsHistory;
    }

    private String getDate(Long timestamp){

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();


        return date;
    }


    /**
     * Crearea cererii HTTP de plata a soferului prin care acesta isi va primi banii de la firma.
     */
    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest() {
        progress = new ProgressDialog(this);
        progress.setTitle("Processing Your Payout");
        progress.setMessage("Please wait");
        progress.setCancelable(false);
        progress.show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("email", mPayoutEmail.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        final Request request =new Request.Builder()
                .url("https://us-central1-uberapp-b72f6.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                if (response.isSuccessful()){
                    switch (responseCode){
                        case 200:
                            Snackbar.make(findViewById(R.id.layout), "Payout Successful!", Snackbar.LENGTH_LONG).show();
                            break;
                        case 500:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete Payout", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete Payout", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
                progress.dismiss();
            }
        });
    }

}
