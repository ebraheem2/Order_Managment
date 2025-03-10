package com.example.mangment_order;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangment_order.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OrderAdapter.OnOrderListener {

    private EditText editTextSearch;
    private RecyclerView recyclerViewOrders;
    private Button buttonAddOrder;
    private View logoutView;

    private ArrayList<Order> allOrders = new ArrayList<>();
    private ArrayList<Order> filteredList = new ArrayList<>();
    private OrderAdapter orderAdapter;

    private DatabaseReference ordersRef;
    private ValueEventListener ordersListener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutView = findViewById(R.id.logout);
        logoutView.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Realtime Database reference
        ordersRef = FirebaseDatabase.getInstance("https://mangmentorder-default-rtdb.firebaseio.com/")
                .getReference("orders");

        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        buttonAddOrder = findViewById(R.id.buttonAddOrder);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(filteredList, this);
        recyclerViewOrders.setAdapter(orderAdapter);

        buttonAddOrder.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddOrderActivity.class));
        });

        // Search
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a){}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c){
                filterOrders(s.toString());
            }
            @Override public void afterTextChanged(Editable s){}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachDatabaseReadListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (ordersListener == null) {
            ordersListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    allOrders.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Order order = child.getValue(Order.class);
                        if (order != null) {
                            // Attempt auto-update status
                            autoUpdateStatus(order);
                            allOrders.add(order);
                        }
                    }
                    filterOrders(editTextSearch.getText().toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to read orders: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            };
            ordersRef.addValueEventListener(ordersListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
            ordersListener = null;
        }
    }

    private void filterOrders(String query) {
        filteredList.clear();
        String lower = (query == null) ? "" : query.toLowerCase().trim();
        for (Order o : allOrders) {
            if (lower.isEmpty()
                    || (o.getOrderId() != null && o.getOrderId().toLowerCase().contains(lower))
                    || (o.getItemDescription() != null && o.getItemDescription().toLowerCase().contains(lower))
                    || (o.getOrderName() != null && o.getOrderName().toLowerCase().contains(lower))
                    || (o.getItemNum() != null && o.getItemNum().toLowerCase().contains(lower))
                    || (o.getSource() != null && o.getSource().toLowerCase().contains(lower))
                    || (o.getDestination() != null && o.getDestination().toLowerCase().contains(lower))
            ) {
                filteredList.add(o);
            }
        }
        orderAdapter.notifyDataSetChanged();
    }

    private void autoUpdateStatus(Order order) {
        // If "Arrived & Complete" => do nothing
        if ("Arrived & Complete".equalsIgnoreCase(order.getStatusOfOrder())) {
            return;
        }
        try {
            Date now = new Date();

            // If finalArrivalDate < now => "Arrived & Complete"
            if (order.getFinalArrivalDate() != null) {
                Date finalArrival = sdf.parse(order.getFinalArrivalDate());
                if (finalArrival != null && now.after(finalArrival)) {
                    if (!"Arrived & Complete".equalsIgnoreCase(order.getStatusOfOrder())) {
                        updateStatusInDatabase(order, "Arrived & Complete");
                    }
                    return;
                }
            }
            // If departure + 3 days => "Arrived in Another Country"
            if (order.getDateOfDeparture() != null) {
                Date departure = sdf.parse(order.getDateOfDeparture());
                if (departure != null) {
                    long diff = now.getTime() - departure.getTime();
                    long days = TimeUnit.MILLISECONDS.toDays(diff);

                    if (days >= 3) {
                        if (!"Arrived in Another Country".equalsIgnoreCase(order.getStatusOfOrder())) {
                            updateStatusInDatabase(order, "Arrived in Another Country");
                        }
                    } else if (days >= 0) {
                        if (!"Shipped".equalsIgnoreCase(order.getStatusOfOrder())) {
                            updateStatusInDatabase(order, "Shipped");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusInDatabase(Order order, String newStatus) {
        if (order.getOrderId() == null) return;
        if (newStatus.equalsIgnoreCase(order.getStatusOfOrder())) return;

        ordersRef.child(order.getOrderId()).child("statusOfOrder").setValue(newStatus)
                .addOnFailureListener(e -> {
                    // Log or toast
                });
        // We do not manually refresh because the ValueEventListener will get triggered again
    }

    @Override
    public void onOrderClick(int position) {
        Order selected = filteredList.get(position);
        // Show a popup for "Order Details" or "Shipment Details"
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
        View anchorView = recyclerViewOrders.findViewHolderForAdapterPosition(position).itemView;
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenu().add("Update To Complete");
        popup.getMenu().add("Update");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            if ("Update".equals(item.getTitle()) || "Update To Complete".equals(item.getTitle())) {
                if ("Arrived & Complete".equalsIgnoreCase(selected.getStatusOfOrder())) {
                    new AlertDialog.Builder(this)
                            .setTitle("Order is Complete")
                            .setMessage("This order can no longer be updated or deleted.")
                            .setPositiveButton("OK", null)
                            .show();
                    return false;
                }
                if("Update".equals(item.getTitle())) {
                    // Open UpdateOrderActivity
                    Intent intent = new Intent(MainActivity.this, UpdateOrderActivity.class);
                    intent.putExtra("orderId", selected.getOrderId());
                    startActivity(intent);
                }
                else{
                    // **Set the STATUS** to "Arrived & Complete" (not the itemDescription)
                    selected.setStatusOfOrder("Arrived & Complete");
                    selected.setOrderDate(selected.getFinalArrivalDate());
                    // Write the updated order to the database
                    ordersRef.child(selected.getOrderId()).setValue(selected)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Order status updated!", Toast.LENGTH_SHORT).show();
                                // No need to call finish();
                                // The ValueEventListener will refresh the list automatically
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Update failed.", Toast.LENGTH_SHORT).show();
                            });
                }
                return true;
            }
            else if ("Delete".equals(item.getTitle())) {
                // Confirm delete
                new AlertDialog.Builder(this)
                        .setTitle("Delete Order")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteOrder(selected))
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }


    private void deleteOrder(Order order) {
        ordersRef.child(order.getOrderId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Order deleted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
