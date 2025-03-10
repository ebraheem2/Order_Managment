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

    private TextView textViewShipmentStatus, textViewRemainingTime;
    private DatabaseReference ordersRef;
    private View arrow;

    // Expected date format (adjust if needed)
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipment_detail);

        // Initialize views
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

        // Initialize Firebase Database reference for "orders"
        ordersRef = FirebaseDatabase
                .getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");

        // Retrieve the order ID from the intent extras and load details if available
        String orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadShipmentDetails(orderId);
        }
    }

    private void loadShipmentDetails(String orderId) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Order order = snapshot.getValue(Order.class);
                    if (order != null) {
                        // Set shipment status text
                        textViewShipmentStatus.setText("Shipment Status: " + order.getStatusOfOrder());

                        // Compute remaining time until final arrival date
                        String finalDateStr = order.getFinalArrivalDate();
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
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle potential errors here
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
