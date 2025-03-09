package com.example.mangment_order;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mangment_order.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderListener {
        void onOrderClick(int position);
        void onOrderLongClick(int position);
    }

    private List<Order> orders;
    private OnOrderListener listener;

    public OrderAdapter(List<Order> orders, OnOrderListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.textViewOrderId.setText("Order ID: " + order.getOrderId());
        holder.textViewItemDescription.setText(order.getItemDescription());
        holder.textViewStatus.setText("Status: " + order.getStatusOfOrder());

        if ("Arrived & Complete".equalsIgnoreCase(order.getStatusOfOrder())) {
            holder.imageViewStatusIcon.setImageResource(R.drawable.ic_baseline_check_24);
        } else {
            holder.imageViewStatusIcon.setImageResource(R.drawable.ic_baseline_access_time_24);
        }

        // Fade-in animation
        holder.itemView.setAnimation(
                AnimationUtils.loadAnimation(
                        holder.itemView.getContext(),
                        R.anim.item_fade_in
                )
        );
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewStatusIcon;
        TextView textViewOrderId, textViewItemDescription, textViewStatus;

        public OrderViewHolder(@NonNull View itemView, OnOrderListener listener) {
            super(itemView);
            imageViewStatusIcon = itemView.findViewById(R.id.imageViewStatusIcon);
            textViewOrderId = itemView.findViewById(R.id.textViewOrderId);
            textViewItemDescription = itemView.findViewById(R.id.textViewItemDescription);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onOrderLongClick(getAdapterPosition());
                }
                return true;
            });
        }
    }
}
