package com.example.mangment_order;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AddOrderActivity extends AppCompatActivity {

    private EditText editTextItemNum, editTextItemDescription, editTextSource,
            editTextDestination, editTextNameOrder;
    private Button buttonSaveOrder, buttonCancel;
    private View arrow;

    private DatabaseReference ordersRef, shipmentsRef;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        // Initialize Realtime Database references
        ordersRef = FirebaseDatabase.getInstance(
                "https://mangmentorder-default-rtdb.firebaseio.com/"
        ).getReference("orders");

        shipmentsRef = FirebaseDatabase.getInstance(
                "https://mangmentorder-default-rtdb.firebaseio.com/"
        ).getReference("shipments");

        editTextItemNum = findViewById(R.id.editTextItemNum);
        editTextItemDescription = findViewById(R.id.editTextItemDescription);
        editTextSource = findViewById(R.id.editTextSource);
        editTextNameOrder = findViewById(R.id.editTextItemName);
        editTextDestination = findViewById(R.id.editTextDestination);

        arrow = findViewById(R.id.ArrowBack);
        buttonSaveOrder = findViewById(R.id.buttonSaveOrder);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Arrow back
        arrow.setOnClickListener(v -> {
            startActivity(new Intent(AddOrderActivity.this, MainActivity.class));
            finish();
        });

        buttonSaveOrder.setOnClickListener(v -> checkDuplicatesAndSave());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void checkDuplicatesAndSave() {
        String itemNum = editTextItemNum.getText().toString().trim();
        String itemDesc = editTextItemDescription.getText().toString().trim();
        if (TextUtils.isEmpty(itemNum) || TextUtils.isEmpty(itemDesc)) {
            Toast.makeText(this, "Item # and Description required!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isDigitsOnly(itemNum)){
            Toast.makeText(this,"Item num is must Digit only!",Toast.LENGTH_SHORT).show();
            return ;
        }
        // Build comboKey
        String comboKey = itemNum + "_" + itemDesc;

        // Query: orders where comboKey == that value
        ordersRef.orderByChild("comboKey")
                .equalTo(comboKey)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        // Duplicate found
                        Toast.makeText(AddOrderActivity.this,
                                "An order with these details already exists!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // No duplicates => proceed
                        saveOrder(comboKey);
                    }
                })
                .addOnFailureListener(e -> {
                    // If we get here, it's likely a permission or indexing error
                    Toast.makeText(AddOrderActivity.this,
                            "Error checking duplicates: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveOrder(String comboKey) {
        String itemNum = editTextItemNum.getText().toString().trim();
        String itemDesc = editTextItemDescription.getText().toString().trim();
        String source = editTextSource.getText().toString().trim();
        String destination = editTextDestination.getText().toString().trim();
        String itemName = editTextNameOrder.getText().toString().trim();

        // Generate some date/time fields
        Date now = new Date();
        String orderDate = sdf.format(now);

        // departure date in +1..+2 days
        Calendar cDeparture = Calendar.getInstance();
        cDeparture.setTime(now);
        cDeparture.add(Calendar.DAY_OF_YEAR, 1 + new Random().nextInt(2));
        String dateOfDeparture = sdf.format(cDeparture.getTime());

        // receipt date in +1..+3 hours
        Calendar cReceipt = Calendar.getInstance();
        cReceipt.setTime(now);
        cReceipt.add(Calendar.HOUR_OF_DAY, 1 + new Random().nextInt(3));
        String dateOfReceipt = sdf.format(cReceipt.getTime());

        // final arrival = departure + 7 days
        Calendar cArrival = Calendar.getInstance();
        cArrival.setTime(cDeparture.getTime());
        cArrival.add(Calendar.DAY_OF_YEAR, 7);
        String finalArrivalDate = sdf.format(cArrival.getTime());

        // Make an ID for the new order
        String newOrderId = ordersRef.push().getKey();
        if (newOrderId == null) {
            Toast.makeText(this, "Failed to generate Order ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initial status
        String statusOfOrder = "Pending";
        List<String> shipmentIds = new ArrayList<>();

        // Build the Order object (with comboKey!)
        Order newOrder = new Order(
                newOrderId,          // orderId
                comboKey,            // comboKey
                orderDate,           // orderDate
                itemName,            // orderName
                itemNum,             // itemNum
                itemDesc,            // itemDescription
                source,              // source
                dateOfDeparture,     // dateOfDeparture
                destination,         // destination
                finalArrivalDate,    // finalArrivalDate
                dateOfReceipt,       // dateOfReceipt
                statusOfOrder,       // statusOfOrder
                shipmentIds
        );

        // Save the order
        ordersRef.child(newOrderId).setValue(newOrder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddOrderActivity.this, "Order created!", Toast.LENGTH_SHORT).show();
                    // Automatically create the shipment
                    createShipmentForOrder(newOrderId, dateOfDeparture, finalArrivalDate);

                    // Return to main
                    startActivity(new Intent(AddOrderActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddOrderActivity.this,
                            "Failed to create order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createShipmentForOrder(String orderId, String departureDate, String arrivalDate) {
        // We'll just mirror the date fields
        String newShipmentId = shipmentsRef.push().getKey();
        if (newShipmentId == null) return;

        Shipment newShipment = new Shipment(
                newShipmentId,
                orderId,
                departureDate,
                arrivalDate,
                "Pending" // initial status
        );

        shipmentsRef.child(newShipmentId).setValue(newShipment)
                .addOnSuccessListener(aVoid -> {
                    // The ShipmentListActivity will see it in real-time
                })
                .addOnFailureListener(e -> {
                    // Log or toast if needed
                });
    }
}
