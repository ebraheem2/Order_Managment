package com.example.mangment_order;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mangment_order.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UpdateOrderActivity extends AppCompatActivity {

    private EditText editTextStatus, editTextOrderName, editTextItemNum,
            editTextItemDesc, editTextSource, editTextDestination, editTextDeparture;
    private Button buttonUpdate, buttonDatePicker;

    private DatabaseReference ordersRef;
    private String orderId;
    private View arrow;

    private Order currentOrder;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_order);

        ordersRef = FirebaseDatabase
                .getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");

        editTextStatus = findViewById(R.id.editTextStatusOfOrderUpdate);
        editTextOrderName = findViewById(R.id.editTextOrderName);
        editTextItemNum = findViewById(R.id.editTextItemNumUpdate);
        editTextItemDesc = findViewById(R.id.editTextItemDescUpdate);
        editTextSource = findViewById(R.id.editTextSourceUpdate);
        editTextDestination = findViewById(R.id.editTextDestinationUpdate);
        editTextDeparture = findViewById(R.id.editTextDepartureUpdate);
        arrow=findViewById(R.id.ArrowBack);
        Intent backHome=new Intent(this,MainActivity.class);
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(backHome);
                finish();

            }
        });
        buttonUpdate = findViewById(R.id.buttonUpdateOrder);
        buttonDatePicker = findViewById(R.id.buttonPickDate);

        if (getIntent() != null) {
            orderId = getIntent().getStringExtra("orderId");
            loadOrder(orderId);
        }

        buttonDatePicker.setOnClickListener(v -> pickNewDepartureDate());
        buttonUpdate.setOnClickListener(v -> updateOrder());
    }

    private void loadOrder(String orderId) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        editTextStatus.setText(currentOrder.getStatusOfOrder());
                        editTextOrderName.setText(currentOrder.getOrderName());
                        editTextItemNum.setText(currentOrder.getItemNum());
                        editTextItemDesc.setText(currentOrder.getItemDescription());
                        editTextSource.setText(currentOrder.getSource());
                        editTextDestination.setText(currentOrder.getDestination());
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
        Calendar today = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, month, dayOfMonth, 9, 0); // e.g. 9:00 AM
                    if (newDate.before(today)) {
                        Toast.makeText(this, "Cannot pick a past date!", Toast.LENGTH_SHORT).show();
                    } else {
                        String departureStr = sdf.format(newDate.getTime());
                        editTextDeparture.setText(departureStr);
                    }
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
        dp.getDatePicker().setMinDate(today.getTimeInMillis());
        dp.show();
    }

    private void updateOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "Order not loaded yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        String newStatus = editTextStatus.getText().toString().trim();
        String newOrderName = editTextOrderName.getText().toString().trim();
        String newItemNum = editTextItemNum.getText().toString().trim();
        String newItemDesc = editTextItemDesc.getText().toString().trim();
        String newSource = editTextSource.getText().toString().trim();
        String newDestination = editTextDestination.getText().toString().trim();
        String newDeparture = editTextDeparture.getText().toString().trim();
        if(!TextUtils.isDigitsOnly(newItemNum) && newItemNum.length()>0){
            Toast.makeText(this,"Item num is must Digit only!",Toast.LENGTH_SHORT).show();
            return ;
        }
        if (TextUtils.isEmpty(newDeparture)) {
            Toast.makeText(this, "Departure date is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Recalc finalArrival = departure + 7 days
        String finalArrivalStr = currentOrder.getFinalArrivalDate(); // fallback
        try {
            Date depDate = sdf.parse(newDeparture);
            if (depDate != null) {
                Calendar cArrival = Calendar.getInstance();
                cArrival.setTime(depDate);
                cArrival.add(Calendar.DAY_OF_YEAR, 7);
                finalArrivalStr = sdf.format(cArrival.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Build updated object
        currentOrder.setStatusOfOrder(newStatus);
        currentOrder.setOrderName(newOrderName);
        currentOrder.setItemNum(newItemNum);
        currentOrder.setItemDescription(newItemDesc);
        currentOrder.setSource(newSource);
        currentOrder.setDestination(newDestination);
        currentOrder.setDateOfDeparture(newDeparture);
        currentOrder.setFinalArrivalDate(finalArrivalStr);

        // Update in DB
        ordersRef.child(orderId).setValue(currentOrder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UpdateOrderActivity.this, "Order updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateOrderActivity.this, "Update failed.", Toast.LENGTH_SHORT).show();
                });
        Toast.makeText(UpdateOrderActivity.this, "Order updated!", Toast.LENGTH_SHORT).show();
        Intent backhome=new Intent(this,MainActivity.class);
        startActivity(backhome);
        finish();
    }
}
