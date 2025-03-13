package com.example.mangment_order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ShipmentDetailActivity extends AppCompatActivity {

    private TextView textViewShipmentId, textViewOrderId, textViewDepartureDate, textViewClock,
            textViewFinalArrivalDate, textViewShipmentStatus, textViewRemainingTime;
    private DatabaseReference shipmentsRef;
    private View arrow;

    // Expected date format (adjust if needed)
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipment_detail);

        // Initialize views
        textViewShipmentId = findViewById(R.id.textViewShipmentId);
        textViewOrderId = findViewById(R.id.textViewOrderId);
        textViewDepartureDate = findViewById(R.id.textViewDepartureDate);
        textViewClock = findViewById(R.id.textViewClock);
        textViewFinalArrivalDate = findViewById(R.id.textViewFinalArrivalDate);
        textViewShipmentStatus = findViewById(R.id.textViewShipmentStatus);
        textViewRemainingTime = findViewById(R.id.textViewRemainingTime);
        arrow = findViewById(R.id.ArrowBack);

        // Set click listener for the back arrow
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backHome = new Intent(ShipmentDetailActivity.this, MainActivity.class);
                startActivity(backHome);
                finish();
            }
        });

        // Initialize Firebase Database reference for "shipments"
        shipmentsRef = FirebaseDatabase
                .getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("shipments");

        // Retrieve the order ID from the intent extras (passed from MainActivity)
        String orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadShipmentDetails(orderId);
        }
    }

    private void loadShipmentDetails(String orderId) {
        // Query shipments where the "orderId" field matches the passed orderId
        shipmentsRef.orderByChild("orderId").equalTo(orderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Assuming one shipment per order. Iterate over results.
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Shipment shipment = child.getValue(Shipment.class);
                                if (shipment != null) {
                                    // Populate all shipment details
                                    textViewShipmentId.setText("Shipment ID: " + shipment.getShipmentId());
                                    textViewOrderId.setText("Order ID: " + shipment.getOrderId());
                                    textViewDepartureDate.setText("Departure Date: " + shipment.getDepartureDate());
                                    textViewClock.setText("Clock: " + shipment.getClock());
                                    textViewFinalArrivalDate.setText("Final Arrival Date: " + shipment.getFinalArrivalDate());
                                    textViewShipmentStatus.setText("Shipment Status: " + shipment.getStatusOfShipment());

                                    // Compute remaining time until final arrival date
                                    String finalDateStr = shipment.getFinalArrivalDate();
                                    if (finalDateStr != null) {
                                        try {
                                            Date finalDate = sdf.parse(finalDateStr);
                                            Date now = new Date();
                                            if (finalDate != null) {
                                                long diff = finalDate.getTime() - now.getTime();
                                                if (diff <= 0) {
                                                    textViewRemainingTime.setText("Remaining Time: Already Arrived");
                                                } else {
                                                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                                                    long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                                                    textViewRemainingTime.setText("Remaining Time: " + days + " days, " + hours + " hours");
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            textViewRemainingTime.setText("Error parsing date");
                                        }
                                    }
                                    // Break after processing the first shipment found
                                    break;
                                }
                            }
                        } else {
                            textViewShipmentStatus.setText("No shipment details found for this order.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Handle potential errors here if needed.
                    }
                });
    }

    @Override
    public void finish() {
        super.finish();
        // Apply custom transition animation (ensure these files exist in res/anim/)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
