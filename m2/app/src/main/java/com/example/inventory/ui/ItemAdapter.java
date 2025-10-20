package com.example.inventory.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory.R;
import com.example.inventory.model.Item;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
    private final List<Item> data = new ArrayList<>();

    public void submitList(List<Item> newData) {
        data.clear();
        if (newData != null) data.addAll(newData);
        notifyDataSetChanged(); // fine for this milestone
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Item it = data.get(pos);
        h.name.setText(it.name);
        h.qty.setText(String.valueOf(it.quantity));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, qty;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            qty  = v.findViewById(R.id.tvQty);
        }
    }
}
