package com.example.mangment_order;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangment_order.Shipment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ShipmentListActivity extends AppCompatActivity
        implements ShipmentAdapter.OnShipmentListener {

    private RecyclerView recyclerViewShipments;
    private ShipmentAdapter shipmentAdapter;
    private List<Shipment> allShipments = new ArrayList<>();

    private DatabaseReference shipmentsRef;
    private ValueEventListener shipmentsListener;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipment_list);

        recyclerViewShipments = findViewById(R.id.recyclerViewShipments);
        recyclerViewShipments.setLayoutManager(new LinearLayoutManager(this));

        shipmentAdapter = new ShipmentAdapter(allShipments, this);
        recyclerViewShipments.setAdapter(shipmentAdapter);

        // Realtime Database reference:
        shipmentsRef = FirebaseDatabase
                .getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("shipments");
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachShipmentListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachShipmentListener();
    }

    private void attachShipmentListener() {
        if (shipmentsListener == null) {
            shipmentsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    allShipments.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Shipment s = child.getValue(Shipment.class);
                        if (s != null) {
                            // Attempt auto-update
                            autoUpdateShipmentStatus(s);
                            allShipments.add(s);
                        }
                    }
                    shipmentAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ShipmentListActivity.this,
                            "Failed to read shipments: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            };
            shipmentsRef.addValueEventListener(shipmentsListener);
        }
    }

    private void detachShipmentListener() {
        if (shipmentsListener != null) {
            shipmentsRef.removeEventListener(shipmentsListener);
            shipmentsListener = null;
        }
    }

    /**
     * autoUpdateShipmentStatus logic:
     * If finalArrivalDate < now => "Arrived"
     * else if departureDate < now => "In-Transit"
     * else => "Pending"
     */
    private void autoUpdateShipmentStatus(Shipment shipment) {
        String currentStatus = shipment.getStatusOfShipment();
        if (currentStatus == null) currentStatus = "Pending";

        // If it's already "Arrived", do nothing
        if ("Arrived".equalsIgnoreCase(currentStatus)) return;

        try {
            Date now = new Date();

            // 1) If finalArrivalDate in past => "Arrived"
            if (shipment.getFinalArrivalDate() != null) {
                Date finalDate = sdf.parse(shipment.getFinalArrivalDate());
                if (finalDate != null && now.after(finalDate)) {
                    if (!"Arrived".equalsIgnoreCase(currentStatus)) {
                        updateShipmentStatus(shipment, "Arrived");
                    }
                    return;
                }
            }

            // 2) If departureDate in past => "In-Transit"
            if (shipment.getDepartureDate() != null) {
                Date dep = sdf.parse(shipment.getDepartureDate());
                if (dep != null && now.after(dep)) {
                    if (!"In-Transit".equalsIgnoreCase(currentStatus)) {
                        updateShipmentStatus(shipment, "In-Transit");
                    }
                    return;
                }
            }

            // 3) Otherwise => "Pending"
            if (!"Pending".equalsIgnoreCase(currentStatus)) {
                updateShipmentStatus(shipment, "Pending");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Actually updates the status in RTDB, if different from the current value.
     */
    private void updateShipmentStatus(Shipment shipment, String newStatus) {
        if (shipment.getShipmentId() == null) return;
        if (newStatus.equalsIgnoreCase(shipment.getStatusOfShipment())) {
            return; // no-op
        }

        shipmentsRef.child(shipment.getShipmentId())
                .child("statusOfShipment")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    // We'll see the new status in the next onDataChange callback
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ShipmentListActivity.this,
                            "Failed to update shipment status.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Implement interface methods if you want to handle item clicks or long-clicks:

    @Override
    public void onShipmentClick(int position) {
        Shipment s = allShipments.get(position);
        // e.g. open a detail screen or show a popup
        Toast.makeText(this, "Clicked " + s.getShipmentId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShipmentLongClick(int position) {
        Shipment s = allShipments.get(position);
        // e.g. show a popup to edit or delete
        Toast.makeText(this, "Long-click " + s.getShipmentId(), Toast.LENGTH_SHORT).show();
    }
}
