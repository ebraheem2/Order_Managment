package com.example.mangment_order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangment_order.Shipment;

import java.util.List;

public class ShipmentAdapter extends RecyclerView.Adapter<ShipmentAdapter.ShipmentViewHolder> {

    private List<Shipment> shipmentList;

    // Optional: define interface if you want click or long-press handling
    public interface OnShipmentListener {
        void onShipmentClick(int position);
        void onShipmentLongClick(int position);
    }

    private OnShipmentListener listener;

    public ShipmentAdapter(List<Shipment> shipmentList, OnShipmentListener listener) {
        this.shipmentList = shipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shipment, parent, false);
        return new ShipmentViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ShipmentViewHolder holder, int position) {
        Shipment shipment = shipmentList.get(position);
        holder.textViewShipmentId.setText("Shipment ID: " + shipment.getShipmentId());
        holder.textViewStatus.setText("Status: " + shipment.getStatusOfShipment());
        holder.textViewDeparture.setText("Departure: " + shipment.getDepartureDate());
        holder.textViewArrival.setText("Final Arrival: " + shipment.getFinalArrivalDate());
    }

    @Override
    public int getItemCount() {
        return shipmentList.size();
    }

    static class ShipmentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewShipmentId, textViewStatus, textViewDeparture, textViewArrival;

        public ShipmentViewHolder(@NonNull View itemView, OnShipmentListener listener) {
            super(itemView);
            textViewShipmentId = itemView.findViewById(R.id.textViewShipmentId);
            textViewStatus = itemView.findViewById(R.id.textViewShipmentStatus);
            textViewDeparture = itemView.findViewById(R.id.textViewDeparture);
            textViewArrival = itemView.findViewById(R.id.textViewFinalArrival);

            // If you want click events
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShipmentClick(getAdapterPosition());
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onShipmentLongClick(getAdapterPosition());
                }
                return true;
            });
        }
    }
}
