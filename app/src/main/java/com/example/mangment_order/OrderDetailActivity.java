package com.example.mangment_order;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mangment_order.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView textViewOrderId, textViewOrderDate, textViewName,
            textViewItemNum, textViewItemDesc, textViewSource,
            textViewDeparture, textViewDestination, textViewEstimatedArrival,
            textViewReceipt, textViewStatusOfOrder;
    private Button buttonUpdateOrder, buttonDeleteOrder;
    private View arrow;

    private DatabaseReference ordersRef;
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        ordersRef = FirebaseDatabase
                .getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");

        arrow = findViewById(R.id.ArrowBack);
        arrow.setOnClickListener(v -> {
            startActivity(new Intent(OrderDetailActivity.this, MainActivity.class));
            finish();
        });

        textViewOrderId = findViewById(R.id.textViewOrderId);
        textViewOrderDate = findViewById(R.id.textViewOrderDate);
        textViewName = findViewById(R.id.textViewName);
        textViewItemNum = findViewById(R.id.textViewItemNum);
        textViewItemDesc = findViewById(R.id.textViewItemDesc);
        textViewSource = findViewById(R.id.textViewSource);
        textViewDeparture = findViewById(R.id.textViewDeparture);
        textViewDestination = findViewById(R.id.textViewDestination);
        textViewEstimatedArrival = findViewById(R.id.textViewEstimatedArrival);
        textViewReceipt = findViewById(R.id.textViewReceipt);
        textViewStatusOfOrder = findViewById(R.id.textViewStatusOfOrder);

        buttonUpdateOrder = findViewById(R.id.buttonUpdateOrder);
        buttonDeleteOrder = findViewById(R.id.buttonDeleteOrder);

        if (getIntent() != null) {
            orderId = getIntent().getStringExtra("orderId");
            if (!TextUtils.isEmpty(orderId)) {
                loadOrderDetails(orderId);
            }
        }

        buttonUpdateOrder.setOnClickListener(v -> {
            if (currentOrder == null) return;
            if ("Received".equalsIgnoreCase(currentOrder.getStatusOfOrder().getStatus())) {
                Toast.makeText(this, "Cannot update a completed order.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Go to the update activity
            Intent intent = new Intent(OrderDetailActivity.this, UpdateOrderActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
            finish();
        });

        buttonDeleteOrder.setOnClickListener(v -> {
            if (currentOrder == null) return;
            if ("Received".equalsIgnoreCase(currentOrder.getStatusOfOrder().getStatus())) {
                Toast.makeText(this, "Cannot delete a completed order.", Toast.LENGTH_SHORT).show();
                return;
            }
            deleteOrder();
        });
    }

    private void loadOrderDetails(String orderId) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        textViewOrderId.setText("Order ID: " + currentOrder.getOrderId());
                        textViewOrderDate.setText("Order Date: " + currentOrder.getOrderDate());
                        textViewName.setText("Names: " + currentOrder.getOrderName());
                        textViewItemNum.setText("Item #: " + currentOrder.getItemNum());
                        textViewItemDesc.setText("Description: " + currentOrder.getItemDescription());
                        textViewSource.setText("Source: " + currentOrder.getSource());
                        textViewDeparture.setText("Departure: " + currentOrder.getDateOfDeparture());
                        textViewDestination.setText("Destination: " + currentOrder.getDestination());
                        textViewEstimatedArrival.setText("Final Arrival: " + currentOrder.getFinalArrivalDate());
                        textViewReceipt.setText("Receipt: " + currentOrder.getDateOfReceipt());
                        textViewStatusOfOrder.setText("Status: " + currentOrder.getStatusOfOrder());

                        // If complete => disable update/delete
                        if ("Received".equalsIgnoreCase(currentOrder.getStatusOfOrder().getStatus())) {
                            buttonUpdateOrder.setEnabled(false);
                            buttonDeleteOrder.setEnabled(false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OrderDetailActivity.this, "Failed to load order.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteOrder() {
        ordersRef.child(orderId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(OrderDetailActivity.this, "Order deleted!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(OrderDetailActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(OrderDetailActivity.this, "Failed to delete order.", Toast.LENGTH_SHORT).show()
                );
    }
}
