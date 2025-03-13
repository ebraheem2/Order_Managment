package com.example.mangment_order;

import java.util.List;

public class Order {
    private String orderId;
    private String comboKey; // For duplicates: itemNum + "_" + itemDescription
    private String orderDate;
    private String orderName;
    private String itemNum;
    private String itemDescription;
    private Countries source;
    private String dateOfDeparture;
    private Countries destination;
    private String finalArrivalDate;
    private String dateOfReceipt;
    private StatusOrders statusOfOrder;
    private String shipmentId; // if you want to store shipments

    public Order() {
        // Required empty constructor for Firebase
    }

    // Full constructor
    public Order(String orderId,
                 String comboKey,
                 String orderDate,
                 String orderName,
                 String itemNum,
                 String itemDescription,
                 Countries source,
                 String dateOfDeparture,
                 Countries destination,
                 String finalArrivalDate,
                 String dateOfReceipt,

                 String shipmentId) {
        this.orderId = orderId;
        this.comboKey = comboKey;
        this.orderDate = orderDate;
        this.orderName = orderName;
        this.itemNum = itemNum;
        this.itemDescription = itemDescription;
        this.source = source;
        this.dateOfDeparture = dateOfDeparture;
        this.destination = destination;
        this.finalArrivalDate = finalArrivalDate;
        this.dateOfReceipt = dateOfReceipt;
        this.statusOfOrder = StatusOrders.Opened;
        this.shipmentId = shipmentId;
    }

    // Getters/Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getComboKey() { return comboKey; }
    public void setComboKey(String comboKey) { this.comboKey = comboKey; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getOrderName() { return orderName; }
    public void setOrderName(String orderName) { this.orderName = orderName; }

    public String getItemNum() { return itemNum; }
    public void setItemNum(String itemNum) { this.itemNum = itemNum; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public Countries getSource() { return source; }
    public void setSource(Countries source) { this.source = source; }

    public String getDateOfDeparture() { return dateOfDeparture; }
    public void setDateOfDeparture(String dateOfDeparture) { this.dateOfDeparture = dateOfDeparture; }

    public Countries getDestination() { return destination; }
    public void setDestination(Countries destination) { this.destination = destination; }

    public String getFinalArrivalDate() { return finalArrivalDate; }
    public void setFinalArrivalDate(String finalArrivalDate) { this.finalArrivalDate = finalArrivalDate; }

    public String getDateOfReceipt() { return dateOfReceipt; }
    public void setDateOfReceipt(String dateOfReceipt) { this.dateOfReceipt = dateOfReceipt; }

    public StatusOrders getStatusOfOrder() { return statusOfOrder; }
    public void setStatusOfOrder(StatusOrders statusOfOrder) { this.statusOfOrder = statusOfOrder; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
}
