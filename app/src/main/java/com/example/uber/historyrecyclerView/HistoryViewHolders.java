package com.example.uber.historyrecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber.HistorySingleActivity;
import com.example.uber.R;

/**
 *  clasa responsabila de crearea referintelor elementelor grafice de layout-ul inregistrarii
 */
public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView rideId;
    public TextView time;
    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        rideId = (TextView)itemView.findViewById(R.id.rideId);
        time = (TextView)itemView.findViewById(R.id.time);
    }

    /**
     * metoda care raspunde de redirectionarea ctre o noua activitate in momentul in care se da click pe inregistrari.
     */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
