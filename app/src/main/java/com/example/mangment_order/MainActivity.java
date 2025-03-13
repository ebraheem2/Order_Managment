package com.example.mangment_order;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OrderAdapter.OnOrderListener {

    private EditText editTextSearch;
    private RecyclerView recyclerViewOrders;
    private Button buttonAddOrder;
    private View logoutView;

    private ArrayList<Order> allOrders = new ArrayList<>();
    private ArrayList<Shipment> allShipment = new ArrayList<>();
    private ArrayList<Order> filteredList = new ArrayList<>();
    private ArrayList<Shipment> filteredShipment = new ArrayList<>();

    private OrderAdapter orderAdapter;
    private DatabaseReference ordersRef, shipmentsRef;
    private ValueEventListener ordersListener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Handler for scheduling periodic updates every hour.
    private Handler statusUpdateHandler = new Handler();
    // We'll use a single-thread executor to offload auto-update computations.
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Runnable statusUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            // Offload heavy computation to background thread.
            executorService.execute(() -> {
                // Build a lookup map for shipments by shipmentId.
                Map<String, Shipment> shipmentMap = new HashMap<>();
                for (Shipment shipment : allShipment) {
                    if (shipment.getShipmentId() != null) {
                        shipmentMap.put(shipment.getShipmentId(), shipment);
                    }
                }
                // For each order that has a shipment, perform auto update in background.
                for (Order order : allOrders) {
                    if (order.getShipmentId() != null && shipmentMap.containsKey(order.getShipmentId())) {
                        if (!order.getStatusOfOrder().equals(StatusOrders.Cancelled)) {
                            autoUpdateStatus(order, shipmentMap.get(order.getShipmentId()));
                        }
                    }
                }
            });
            // Schedule next update in 1 hour.
            statusUpdateHandler.postDelayed(this, 3600000);
        }
    };

    // Use SharedPreferences to persist last update times.
    private static final String PREFS_NAME = "last_update";

    // BroadcastReceiver to listen for network connectivity changes.
    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable(context)) {
                // Re-attach the database listeners when connectivity returns.
                attachDatabaseReadListener();
            }
        }
    };

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.isConnected());
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // (Optional) Enable Firebase offline persistence (best done in Application class)
        // FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        logoutView = findViewById(R.id.logout);
        logoutView.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        ordersRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");
        shipmentsRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("shipments");

        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        buttonAddOrder = findViewById(R.id.buttonAddOrder);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(filteredList, this);
        recyclerViewOrders.setAdapter(orderAdapter);

        buttonAddOrder.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddOrderActivity.class))
        );

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOrders(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachDatabaseReadListener();
        statusUpdateHandler.post(statusUpdateRunnable);
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
            ordersListener = null;
        }
        statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
        unregisterReceiver(networkReceiver);
    }

    private void attachDatabaseReadListener() {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ordersSnapshot) {
                allOrders.clear();
                for (DataSnapshot child : ordersSnapshot.getChildren()) {
                    Order order = child.getValue(Order.class);
                    if (order != null) {
                        allOrders.add(order);
                    }
                }
                shipmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot shipmentsSnapshot) {
                        allShipment.clear();
                        for (DataSnapshot child : shipmentsSnapshot.getChildren()) {
                            Shipment shipment = child.getValue(Shipment.class);
                            if (shipment != null) {
                                allShipment.add(shipment);
                            }
                        }
                        // Immediately update statuses after data load.
                        Map<String, Shipment> shipmentMap = new HashMap<>();
                        for (Shipment shipment : allShipment) {
                            if (shipment.getShipmentId() != null) {
                                shipmentMap.put(shipment.getShipmentId(), shipment);
                            }
                        }
                        for (Order order : allOrders) {
                            if (order.getShipmentId() != null && shipmentMap.containsKey(order.getShipmentId())) {
                                if (!order.getStatusOfOrder().equals(StatusOrders.Cancelled)) {
                                    autoUpdateStatus(order, shipmentMap.get(order.getShipmentId()));
                                }
                            }
                        }
                        filterOrders(editTextSearch.getText().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,
                                "Failed to read shipments: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Failed to read orders: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        ordersRef.addValueEventListener(ordersListener);
    }

    private void filterOrders(String query) {
        filteredList.clear();
        filteredShipment.clear();
        String lower = (query == null) ? "" : query.toLowerCase().trim();
        for (Order order : allOrders) {
            if (lower.isEmpty()
                    || (order.getOrderId() != null && order.getOrderId().toLowerCase().contains(lower))
                    || (order.getItemDescription() != null && order.getItemDescription().toLowerCase().contains(lower))
                    || (order.getOrderName() != null && order.getOrderName().toLowerCase().contains(lower))
                    || (order.getItemNum() != null && order.getItemNum().toLowerCase().contains(lower))
                    || (order.getSource() != null && order.getSource().toString().toLowerCase().contains(lower))
                    || (order.getDestination() != null && order.getDestination().toString().toLowerCase().contains(lower))
            ) {
                filteredList.add(order);
            }
        }
        // Build filtered shipments list.
        for (Shipment shipment : allShipment) {
            for (Order order : filteredList) {
                if (order.getShipmentId() != null && order.getShipmentId().equals(shipment.getShipmentId())) {
                    filteredShipment.add(shipment);
                    break;
                }
            }
        }
        orderAdapter.notifyDataSetChanged();
    }

    /**
     * Auto-update status runs in the background.
     * It checks receipt, departure, and final arrival conditions and posts Firebase updates on the UI thread.
     */
    private void autoUpdateStatus(final Order order, final Shipment shipment) {
        if (shipment == null || shipment.getStatusOfShipment() == null) {
            Log.e("autoUpdateStatus", "Shipment or its status is null; skipping update.");
            return;
        }
        if (order.getStatusOfOrder().equals(StatusOrders.Cancelled)) {
            return;
        }
        final String key = order.getOrderId() + "_" + shipment.getShipmentId();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastUpdate = prefs.getLong(key, 0);
        final long nowMillis = System.currentTimeMillis();

        try {
            final Date now = new Date();
            // Priority 1: Receipt condition.
            if (order.getDateOfReceipt() != null) {
                Date receiptDate = sdf.parse(order.getDateOfReceipt());
                if (receiptDate != null && now.after(receiptDate)) {
                    if (!shipment.getStatusOfShipment().equals(StatusShipment.DELIVERED)
                            || !order.getStatusOfOrder().equals(StatusOrders.Received)) {
                        runOnUiThread(() -> {
                            updateStatusInDatabase(shipment, StatusShipment.DELIVERED.toString(),
                                    order, StatusOrders.Received.getStatus());
                            prefs.edit().putLong(key, nowMillis).apply();
                        });
                    }
                    return;
                }
            }
            // Priority 2: Departure condition.
            if (order.getDateOfDeparture() != null) {
                Date departureDate = sdf.parse(order.getDateOfDeparture());
                if (departureDate != null && now.after(departureDate)) {
                    // For departure branch: if shipment ordinal < 5, then update.
                    if (shipment.getStatusOfShipment().ordinal() < 5) {
                        final StatusShipment expectedShipmentStatus = StatusShipment.values()[shipment.getStatusOfShipment().ordinal() + 1];
                        if (order.getStatusOfOrder().ordinal() < 2) {
                            final StatusOrders expectedOrderStatus = StatusOrders.values()[order.getStatusOfOrder().ordinal() + 1];
                            runOnUiThread(() -> {
                                updateStatusInDatabase(shipment, expectedShipmentStatus.toString(),
                                        order, expectedOrderStatus.getStatus());
                                prefs.edit().putLong(key, nowMillis).apply();
                            });
                            return;
                        }
                    }
                }
            }
            // Priority 3: Arrival condition.
            if (order.getFinalArrivalDate() != null) {
                Date arrivalDate = sdf.parse(order.getFinalArrivalDate());
                Date receiptDate = (order.getDateOfReceipt() != null) ? sdf.parse(order.getDateOfReceipt()) : null;
                if (arrivalDate != null && now.after(arrivalDate) && (receiptDate == null || now.before(receiptDate))) {
                    if (shipment.getStatusOfShipment().ordinal() < 8) {
                        final StatusShipment expectedShipmentStatus = StatusShipment.values()[shipment.getStatusOfShipment().ordinal() + 1];
                        runOnUiThread(() -> {
                            updateStatusInDatabase(shipment, expectedShipmentStatus.toString(),
                                    order, StatusOrders.Arrive_To_Destination.getStatus());
                            prefs.edit().putLong(key, nowMillis).apply();
                        });
                        return;
                    }
                }
            }
            // If none of the conditions are met, no update is performed.
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the statuses in Firebase if they differ from the current ones.
     */
    private void updateStatusInDatabase(Shipment shipment, String newStatus, Order order, String newStatusOrder) {
        if (shipment.getShipmentId() == null || order.getOrderId() == null) return;
        boolean shipmentAlreadyUpdated = newStatus.equalsIgnoreCase(shipment.getStatusOfShipment().toString());
        boolean orderAlreadyUpdated = newStatusOrder.equalsIgnoreCase(order.getStatusOfOrder().toString());
        if (shipmentAlreadyUpdated && orderAlreadyUpdated) return;

        if (!shipmentAlreadyUpdated) {
            shipmentsRef.child(shipment.getShipmentId()).child("statusOfShipment").setValue(newStatus)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(MainActivity.this, "Status Shipment updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(MainActivity.this, "Failed to update shipment status.", Toast.LENGTH_SHORT).show());
        }
        if (!orderAlreadyUpdated) {
            ordersRef.child(order.getOrderId()).child("statusOfOrder").setValue(newStatusOrder)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(MainActivity.this, "Status Order updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(MainActivity.this, "Failed to update order status.", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onOrderClick(int position) {
        Order selected = filteredList.get(position);
        View anchorView = recyclerViewOrders.findViewHolderForAdapterPosition(position).itemView;
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenu().add("Order Details");
        popup.getMenu().add("Shipment Details");
        popup.setOnMenuItemClickListener(item -> {
            if ("Order Details".equals(item.getTitle())) {
                Intent intent = new Intent(MainActivity.this, OrderDetailActivity.class);
                intent.putExtra("orderId", selected.getOrderId());
                startActivity(intent);
                return true;
            } else if ("Shipment Details".equals(item.getTitle())) {
                Intent intent = new Intent(MainActivity.this, ShipmentDetailActivity.class);
                intent.putExtra("orderId", selected.getOrderId());
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public void onOrderLongClick(int position) {
        Order selected = filteredList.get(position);
        // Find matching shipment using the filtered shipments list.
        Shipment tempShipment = null;
        for (Shipment ship : filteredShipment) {
            if (selected.getShipmentId() != null && selected.getShipmentId().equals(ship.getShipmentId())) {
                tempShipment = ship;
                break;
            }
        }
        final Shipment selectedShipment = tempShipment;
        if (selectedShipment == null) {
            Toast.makeText(this, "No shipment found for this order.", Toast.LENGTH_SHORT).show();
            return;
        }
        View anchorView = recyclerViewOrders.findViewHolderForAdapterPosition(position).itemView;
        PopupMenu popup = new PopupMenu(this, anchorView);
        // Existing update options:
        popup.getMenu().add("UpdateOrder");
        popup.getMenu().add("Update To Complete");
        popup.getMenu().add("Update Shipment Status");
        popup.getMenu().add("Delete");
        // New options:
        popup.getMenu().add("Cancel Order");
        popup.getMenu().add("Resume Order");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("UpdateOrder".equals(title) || "Update To Complete".equals(title)) {
                if ("Received".equalsIgnoreCase(selected.getStatusOfOrder().getStatus())) {
                    new AlertDialog.Builder(this)
                            .setTitle("Order is Complete")
                            .setMessage("This order can no longer be updated or deleted.")
                            .setPositiveButton("OK", null)
                            .show();
                    return false;
                }
                if ("UpdateOrder".equals(title)) {
                    Intent intent = new Intent(MainActivity.this, UpdateOrderActivity.class);
                    intent.putExtra("orderId", selected.getOrderId());
                    startActivity(intent);
                } else {  // Update To Complete
                    selected.setStatusOfOrder(StatusOrders.Received);
                    selected.setOrderDate(selected.getFinalArrivalDate());
                    ordersRef.child(selected.getOrderId()).setValue(selected)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(MainActivity.this, "Order status updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(MainActivity.this, "Update failed.", Toast.LENGTH_SHORT).show());
                }
                return true;
            } else if ("Update Shipment Status".equals(title)) {
                showUpdateStatusDialog(selected, selectedShipment);
                return true;
            } else if ("Delete".equals(title)) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Order")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteOrder(selected, selectedShipment))
                        .setNegativeButton("No", null)
                        .show();
                return true;
            } else if ("Cancel Order".equals(title)) {
                selected.setStatusOfOrder(StatusOrders.Cancelled);
                ordersRef.child(selected.getOrderId()).setValue(selected)
                        .addOnSuccessListener(aVoid -> {
                            String key = selected.getOrderId() + "_" + selectedShipment.getShipmentId();
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
                            Toast.makeText(MainActivity.this, "Order cancelled!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(MainActivity.this, "Failed to cancel order.", Toast.LENGTH_SHORT).show());
                return true;
            } else if ("Resume Order".equals(title)) {
                if (!selected.getStatusOfOrder().equals(StatusOrders.Cancelled)) {
                    Toast.makeText(MainActivity.this, "Order is not cancelled.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                String key = selected.getOrderId() + "_" + selectedShipment.getShipmentId();
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                long cancelTime = prefs.getLong(key, 0);
                selected.setStatusOfOrder(StatusOrders.Opened);
                prefs.edit().remove(key).apply();
                ordersRef.child(selected.getOrderId()).setValue(selected)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(MainActivity.this, "Order resumed!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(MainActivity.this, "Failed to resume order.", Toast.LENGTH_SHORT).show());
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showUpdateStatusDialog(final Order order, final Shipment shipment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Shipment & Order Status");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_status, null);
        builder.setView(dialogView);

        Spinner spinnerShipmentStatus = dialogView.findViewById(R.id.spinnerShipmentStatus);
        Spinner spinnerOrderStatus = dialogView.findViewById(R.id.spinnerOrderStatus);

        ArrayAdapter<StatusOrders> orderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, StatusOrders.values());
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderStatus.setAdapter(orderAdapter);
        spinnerOrderStatus.setSelection(order.getStatusOfOrder().ordinal());

        StatusOrders initialOrderStatus = (StatusOrders) spinnerOrderStatus.getSelectedItem();
        StatusShipment[] allowedStatuses = getAllowedShipmentStatuses(initialOrderStatus);
        ArrayAdapter<StatusShipment> shipmentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, allowedStatuses);
        shipmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShipmentStatus.setAdapter(shipmentAdapter);
        for (int i = 0; i < allowedStatuses.length; i++) {
            if (allowedStatuses[i].equals(shipment.getStatusOfShipment())) {
                spinnerShipmentStatus.setSelection(i);
                break;
            }
        }

        spinnerOrderStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                StatusOrders selectedOrderStatus = (StatusOrders) parent.getItemAtPosition(position);
                StatusShipment[] newAllowed = getAllowedShipmentStatuses(selectedOrderStatus);
                ArrayAdapter<StatusShipment> newAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, newAllowed);
                newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerShipmentStatus.setAdapter(newAdapter);
                if (newAllowed.length > 0) {
                    spinnerShipmentStatus.setSelection(0);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Update", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            StatusShipment newShipmentStatus = (StatusShipment) spinnerShipmentStatus.getSelectedItem();
            StatusOrders newOrderStatus = (StatusOrders) spinnerOrderStatus.getSelectedItem();
            if (newOrderStatus == StatusOrders.Arrive_To_Destination) {
                try {
                    Date now = new Date();
                    Date finalArrival = sdf.parse(order.getFinalArrivalDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            updateStatusInDatabase(shipment, newShipmentStatus.toString(),
                    order, newOrderStatus.getStatus());
            dialog.dismiss();
        });
    }

    private StatusShipment[] getAllowedShipmentStatuses(StatusOrders orderStatus) {
        if (orderStatus == StatusOrders.Opened) {
            return new StatusShipment[]{StatusShipment.PENDING};
        } else if (orderStatus == StatusOrders.Ready_For_Shipment || orderStatus == StatusOrders.Shipped) {
            ArrayList<StatusShipment> list = new ArrayList<>();
            for (StatusShipment s : StatusShipment.values()) {
                if (s.ordinal() >= 1 && s.ordinal() <= 4) {
                    list.add(s);
                }
            }
            return list.toArray(new StatusShipment[0]);
        } else if (orderStatus == StatusOrders.Arrive_To_Destination) {
            ArrayList<StatusShipment> list = new ArrayList<>();
            for (StatusShipment s : StatusShipment.values()) {
                if (s.ordinal() >= 5 && s.ordinal() <= 7) {
                    list.add(s);
                }
            }
            return list.toArray(new StatusShipment[0]);
        } else if (orderStatus == StatusOrders.Received) {
            return new StatusShipment[]{StatusShipment.DELIVERED};
        } else {
            return StatusShipment.values();
        }
    }

    private void deleteOrder(Order order, Shipment ship) {
        ordersRef.child(order.getOrderId()).removeValue()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(MainActivity.this, "Order deleted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Failed to delete order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        shipmentsRef.child(ship.getShipmentId()).removeValue()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(MainActivity.this, "Shipment deleted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Failed to delete shipment: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
