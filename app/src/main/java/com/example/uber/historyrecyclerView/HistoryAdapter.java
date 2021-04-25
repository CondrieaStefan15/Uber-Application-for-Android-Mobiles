package com.example.uber.historyrecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber.R;

import java.util.List;

/**
 * clasa ce raspunde de incarcarea datelor extrase din baza de date pe elementele grafice ale interfetei
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolders> {
    private List<HistoryObject> itemList;
    private Context context;

    /**
     * constructorul prin intermediul caruia setam lista cu obiecte ce contin date privin inregistrarile
     */
    public HistoryAdapter(List<HistoryObject> itemList, Context context){
        this.itemList= itemList;
        this.context = context;
    }

    /**
     * metoda responsabila de incarcarea layout-ului inregistrarii si crearea unui ViewHolder
     */
    @NonNull
    @Override
    public HistoryViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolders rcv = new HistoryViewHolders(layoutView);
        return rcv;
    }

    /**
     * Metoda responsabila de incarcarea datelor pe layout-ul incarcat de metoda de mai sus. Elementele grafice fiind referentiate de catre viewHolder-ul creat mai sus
     */
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolders holder, int position) {
        holder.rideId.setText(itemList.get(getItemCount()-position-1).getRideId());
        holder.time.setText(itemList.get(getItemCount()-position-1).getTime());
    }

    /**
     * Returneaza marimea listei cu obiecte(nr obiectelor reprezinta nr de inregistrari)
     */
    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
