package com.example.mangment_order;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class AddOrderActivity extends AppCompatActivity {

    private EditText editTextItemDescription, editTextNameOrder;
    private Spinner SoruceCountries, DestinationCountries;
    private Button buttonSaveOrder, buttonCancel;
    private View arrow;

    private DatabaseReference ordersRef, shipmentsRef;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        // Initialize Firebase Database references.
        ordersRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/").getReference("orders");
        shipmentsRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/").getReference("shipments");

        SoruceCountries = findViewById(R.id.editTextSource);
        DestinationCountries = findViewById(R.id.editTextDestination);
        Countries[] countries = Countries.values();
        ArrayAdapter<Countries> adapter = new ArrayAdapter<>(this, R.layout.my_spinner_item, countries);
        adapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        ArrayAdapter<Countries> adapter2 = new ArrayAdapter<>(this, R.layout.my_spinner_item, countries);
        adapter2.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        SoruceCountries.setAdapter(adapter);
        DestinationCountries.setAdapter(adapter2);

        editTextItemDescription = findViewById(R.id.editTextItemDescription);
        editTextNameOrder = findViewById(R.id.editTextItemName);

        arrow = findViewById(R.id.ArrowBack);
        buttonSaveOrder = findViewById(R.id.buttonSaveOrder);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Arrow back navigates to MainActivity.
        arrow.setOnClickListener(v -> {
            startActivity(new Intent(AddOrderActivity.this, MainActivity.class));
            finish();
        });

        buttonSaveOrder.setOnClickListener(v -> checkDuplicatesAndSave());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void checkDuplicatesAndSave() {
        String itemDesc = editTextItemDescription.getText().toString().trim();
        String orderName = editTextNameOrder.getText().toString();
        Countries source = (Countries) SoruceCountries.getSelectedItem();
        Countries destination = (Countries) DestinationCountries.getSelectedItem();

        if (TextUtils.isEmpty(itemDesc) || TextUtils.isEmpty(orderName)) {
            Toast.makeText(this, "Please Fill all Fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemDesc.length() > 30 || orderName.length() > 30) {
            Toast.makeText(this, "Description or Order Name can't be more than 30 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a new order ID using a push key and take its first 10 characters.
        String newItemId = Objects.requireNonNull(ordersRef.push().getKey()).substring(0, 10);
        // Build comboKey as newItemId + "_" + itemDesc.
        String comboKey = newItemId + "_" + itemDesc;

        // Check for duplicates in orders using the comboKey.
        ordersRef.orderByChild("comboKey")
                .equalTo(comboKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(AddOrderActivity.this,
                                    "An order with these details already exists!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Pass newItemId along with comboKey so we can extract description reliably.
                            saveOrder(comboKey, newItemId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(AddOrderActivity.this,
                                "Error checking duplicates: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveOrder(String comboKey, String newItemId) {
        // Instead of splitting on "_" (which may appear in itemDesc), use substring.
        String itemNum = newItemId;  // Use newItemId as item number.
        // Extract the description from comboKey by taking substring after the first underscore.
        String itemDesc = comboKey.substring(newItemId.length() + 1);

        Countries source = (Countries) SoruceCountries.getSelectedItem();
        Countries destination = (Countries) DestinationCountries.getSelectedItem();
        String orderName = editTextNameOrder.getText().toString().trim();

        // Generate date/time fields.
        Date now = new Date();
        String orderDate = sdf.format(now);

        Calendar cDeparture = Calendar.getInstance();
        cDeparture.setTime(now);
        cDeparture.add(Calendar.MINUTE, new Random().nextInt(15)+2);
        String clock = cDeparture.getTime().toString();
        String dateOfDeparture = sdf.format(cDeparture.getTime());

        Calendar cArrival = Calendar.getInstance();
        cArrival.setTime(cDeparture.getTime());
        cArrival.add(Calendar.DAY_OF_YEAR, 2 + new Random().nextInt(7));
        String finalArrivalDate = sdf.format(cArrival.getTime());

        Calendar cReceipt = Calendar.getInstance();
        cReceipt.setTime(cArrival.getTime());
        cReceipt.add(Calendar.HOUR_OF_DAY, 1 + new Random().nextInt(3));
        String dateOfReceipt = sdf.format(cReceipt.getTime());

        // Generate new order ID and shipment ID.
        String newOrderId = ordersRef.push().getKey();
        if (newOrderId == null) {
            Toast.makeText(this, "Failed to generate Order ID", Toast.LENGTH_SHORT).show();
            return;
        }
        String shipmentIds = shipmentsRef.push().getKey();
        if (shipmentIds == null) {
            Toast.makeText(this, "Failed to generate Shipment ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the Order object.
        Order newOrder = new Order(
                newOrderId,          // orderId
                comboKey,            // comboKey
                orderDate,           // orderDate
                orderName,           // orderName
                itemNum,             // itemNum
                itemDesc,            // itemDescription
                source,              // source
                dateOfDeparture,     // dateOfDeparture
                destination,         // destination
                finalArrivalDate,    // finalArrivalDate
                dateOfReceipt,       // dateOfReceipt
                shipmentIds          // shipmentId
        );

        ordersRef.child(newOrderId).setValue(newOrder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddOrderActivity.this, "Order created!", Toast.LENGTH_SHORT).show();
                    // Automatically create the shipment.
                    createShipmentForOrder(shipmentIds, newOrderId, dateOfDeparture, clock, finalArrivalDate);
                    // Return to main.
                    startActivity(new Intent(AddOrderActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddOrderActivity.this,
                            "Failed to create order: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createShipmentForOrder(String shipmentIds, String orderId, String departureDate, String clock, String arrivalDate) {
        // Create a new Shipment object. The Shipment constructor sets initial status (e.g., PENDING).
        Shipment newShipment = new Shipment(
                shipmentIds,
                orderId,
                departureDate,
                clock,
                arrivalDate
        );
        Log.e("Print Status Shipment", newShipment.getStatusOfShipment().toString());
        shipmentsRef.child(shipmentIds).setValue(newShipment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Success inserting Shipment Data", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed inserting Shipment Data", Toast.LENGTH_LONG).show();
                });
    }
}
