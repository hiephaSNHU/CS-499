package com.example.inventory.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory.R;
import com.example.inventory.model.Item;

public class ItemAdapter extends ListAdapter<Item, ItemAdapter.VH> {

    public interface OnItemClick {
        void onClick(Item item);
    }

    private final OnItemClick clickListener;

    public ItemAdapter(OnItemClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
        setHasStableIds(true); // helps RecyclerView optimize
    }

    // Stable IDs: improves animations and reduces jank
    @Override
    public long getItemId(int position) {
        Item i = getItem(position);
        return i != null ? i.getId() : RecyclerView.NO_ID;
    }

    public static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldI, @NonNull Item newI) {
            return oldI.getId() == newI.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull Item oldI, @NonNull Item newI) {
            // relies on your Item.equals()
            return oldI.equals(newI);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);
        return new VH(v, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView qty;
        private Item bound;

        VH(@NonNull View itemView, OnItemClick click) {
            super(itemView);
            name = itemView.findViewById(R.id.tvName);
            qty  = itemView.findViewById(R.id.tvQty);

            itemView.setOnClickListener(v -> {
                if (bound != null && click != null) click.onClick(bound);
            });
        }

        void bind(Item item) {
            bound = item;
            name.setText(item.getName());
            qty.setText(String.valueOf(item.getQuantity()));
        }
    }
}
