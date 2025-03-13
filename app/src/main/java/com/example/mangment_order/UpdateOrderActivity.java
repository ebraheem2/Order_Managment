package com.example.mangment_order;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class UpdateOrderActivity extends AppCompatActivity {

    private EditText editTextOrderName, editTextItemDesc, editTextDeparture;
    private Button buttonUpdate, buttonDatePicker;
    private Spinner StatusOrder, Source, Distination;
    private DatabaseReference ordersRef, shipmentsRef;
    private String orderId;
    private View arrow;
    private Order currentOrder;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_order);

        // Initialize database references for orders and shipments.
        ordersRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");
        shipmentsRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("shipments");

        // Find views.
        StatusOrder = findViewById(R.id.editTextStatusOfOrderUpdate);
        editTextOrderName = findViewById(R.id.editTextOrderName);
        editTextItemDesc = findViewById(R.id.editTextItemDescUpdate);
        Source = findViewById(R.id.SourceSpinner);
        Distination = findViewById(R.id.DestinationSpinner);
        editTextDeparture = findViewById(R.id.editTextDepartureUpdate);
        arrow = findViewById(R.id.ArrowBack);
        buttonUpdate = findViewById(R.id.buttonUpdateOrder);
        buttonDatePicker = findViewById(R.id.buttonPickDate);

        // Back arrow navigates to MainActivity.
        arrow.setOnClickListener(v -> {
            startActivity(new Intent(UpdateOrderActivity.this, MainActivity.class));
            finish();
        });

        // Get orderId from the intent and load order data.
        if (getIntent() != null) {
            orderId = getIntent().getStringExtra("orderId");
            loadOrder(orderId);
        }

        buttonDatePicker.setOnClickListener(v -> pickNewDepartureDate());
        buttonUpdate.setOnClickListener(v -> updateOrder());

        // Setup StatusOrder spinner.
        StatusOrders[] statusValues = StatusOrders.values();
        ArrayAdapter<StatusOrders> statusAdapter = new ArrayAdapter<>(this,
                R.layout.my_spinner_item, statusValues);
        statusAdapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        StatusOrder.setAdapter(statusAdapter);

        // Setup source and destination spinners.
        Countries[] sourcesCountries = Countries.values();
        Countries[] distinationCountries = Countries.values();
        ArrayAdapter<Countries> sourceAdapter = new ArrayAdapter<>(this,
                R.layout.my_spinner_item, sourcesCountries);
        ArrayAdapter<Countries> distinationAdapter = new ArrayAdapter<>(this,
                R.layout.my_spinner_item, distinationCountries);
        sourceAdapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        distinationAdapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        Source.setAdapter(sourceAdapter);
        Distination.setAdapter(distinationAdapter);
    }

    private void loadOrder(String orderId) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        StatusOrder.setSelection(currentOrder.getStatusOfOrder().ordinal());
                        editTextOrderName.setText(currentOrder.getOrderName());
                        editTextItemDesc.setText(currentOrder.getItemDescription());
                        Source.setSelection(currentOrder.getSource().ordinal());
                        Distination.setSelection(currentOrder.getDestination().ordinal());
                        editTextDeparture.setText(currentOrder.getDateOfDeparture());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UpdateOrderActivity.this, "Failed to load order.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickNewDepartureDate() {
        final Calendar today = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // When the date is set, show the TimePickerDialog.
            final Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
                // Combine date and time.
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                if (selectedDate.before(today)) {
                    Toast.makeText(this, "Cannot pick a past date/time!", Toast.LENGTH_SHORT).show();
                } else {
                    String departureStr = sdf.format(selectedDate.getTime());
                    editTextDeparture.setText(departureStr);
                }
            }, today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE), true).show();
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void updateOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "Order not loaded yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve updated values from UI.
        StatusOrders newStatusOrder = (StatusOrders) StatusOrder.getSelectedItem();
        String newOrderName = editTextOrderName.getText().toString().trim();
        String newItemDesc = editTextItemDesc.getText().toString().trim();
        Countries newSource = (Countries) Source.getSelectedItem();
        Countries newDestination = (Countries) Distination.getSelectedItem();
        String newDeparture = editTextDeparture.getText().toString().trim();

        if (TextUtils.isEmpty(newDeparture)) {
            Toast.makeText(this, "Departure date is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Use the picked departure date as base.
            Date departureBase = sdf.parse(newDeparture);
            if (departureBase == null) {
                departureBase = new Date();
            }

            // Calculate new departure date and related values:
            // Add random minutes (0 to 14) to departureBase.
            Calendar cDeparture = Calendar.getInstance();
            cDeparture.setTime(departureBase);
            String clock = cDeparture.getTime().toString();
            String dateOfDeparture = sdf.format(cDeparture.getTime());

            // Final arrival = departure + (2 to 8) days.
            Calendar cArrival = Calendar.getInstance();
            cArrival.setTime(departureBase);
            cArrival.add(Calendar.DAY_OF_YEAR, 2 + new Random().nextInt(7));
            String finalArrivalDate = sdf.format(cArrival.getTime());

            // Receipt date = final arrival + (1 to 3) hours.
            Calendar cReceipt = Calendar.getInstance();
            cReceipt.setTime(cArrival.getTime());
            cReceipt.add(Calendar.HOUR_OF_DAY, 1 + new Random().nextInt(3));
            String dateOfReceipt = sdf.format(cReceipt.getTime());

            // Update the currentOrder object with the new values.
            currentOrder.setStatusOfOrder(newStatusOrder);
            currentOrder.setOrderName(newOrderName);
            currentOrder.setItemDescription(newItemDesc);
            currentOrder.setSource(newSource);
            currentOrder.setDestination(newDestination);
            currentOrder.setDateOfDeparture(dateOfDeparture);
            currentOrder.setFinalArrivalDate(finalArrivalDate);
            // Optionally store clock and receipt in the order if needed.

            // Write updated order to Firebase.
            ordersRef.child(orderId).setValue(currentOrder)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UpdateOrderActivity.this, "Order updated!", Toast.LENGTH_SHORT).show();

                        // If the order has a linked shipment, update its dates as well.
                        if (currentOrder.getShipmentId() != null) {
                            DatabaseReference shipmentRef = shipmentsRef.child(currentOrder.getShipmentId());
                            Map<String, Object> shipmentUpdates = new HashMap<>();
                            shipmentUpdates.put("departureDate", dateOfDeparture);
                            shipmentUpdates.put("clock", clock);
                            shipmentUpdates.put("finalArrivalDate", finalArrivalDate);
                            shipmentUpdates.put("dateOfReceipt", dateOfReceipt);
                            shipmentRef.updateChildren(shipmentUpdates)
                                    .addOnSuccessListener(aVoid2 ->
                                            Toast.makeText(UpdateOrderActivity.this, "Shipment updated!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(UpdateOrderActivity.this, "Shipment update failed.", Toast.LENGTH_SHORT).show());
                        }

                        // Return to MainActivity.
                        startActivity(new Intent(UpdateOrderActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(UpdateOrderActivity.this, "Update failed.", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error in updating dates.", Toast.LENGTH_SHORT).show();
        }
    }
}
