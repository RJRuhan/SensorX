package com.example.sensorx;

// ListAdapter.java

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    public List<ListItem> items;
    private final MainActivity mainActivity;

    public void disableButton() {
        for( int position = 0;position < items.size();position++ ) {
            items.get(position).isButtonEnabled = false;
            notifyItemChanged(position);
        }
    }

    public void enableButton() {
        for( int position = 0;position < items.size();position++ ) {
            items.get(position).isButtonEnabled = true;
            notifyItemChanged(position);
        }
    }

    public ListAdapter(List<ListItem> items,MainActivity mainActivity) {
        this.items = items;
        this.mainActivity = mainActivity;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private Button button;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            button = itemView.findViewById(R.id.button);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        ListItem item = items.get(position);
        holder.textView.setText(item.getText());
        holder.button.setEnabled(item.isButtonEnabled);


        // Set click listener for the button
        holder.button.setOnLongClickListener(v -> {
//            Log.d("Main", holder.getBindingAdapterPosition() + " " + items.size());
            mainActivity.sendDeleteCommand(holder.getBindingAdapterPosition());
            items.remove(holder.getBindingAdapterPosition());
            notifyItemRemoved(holder.getBindingAdapterPosition());
            notifyItemRangeChanged(holder.getBindingAdapterPosition(), items.size()-holder.getBindingAdapterPosition());
            return false;
        });
    }

    public void clear() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
