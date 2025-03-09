package com.example.mangment_order;

public class Shipment {
    private String shipmentId;        // The unique key in RTDB
    private String orderId;           // (Optional) which order this belongs to
    private String departureDate;
    private String finalArrivalDate;
    private String statusOfShipment;  // e.g. "Pending", "In-Transit", "Arrived"

    public Shipment() {
        // Needed for Firebase deserialization
    }

    public Shipment(String shipmentId,
                    String orderId,
                    String departureDate,
                    String finalArrivalDate,
                    String statusOfShipment) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.departureDate = departureDate;
        this.finalArrivalDate = finalArrivalDate;
        this.statusOfShipment = statusOfShipment;
    }

    // Getters / Setters
    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getDepartureDate() { return departureDate; }
    public void setDepartureDate(String departureDate) { this.departureDate = departureDate; }

    public String getFinalArrivalDate() { return finalArrivalDate; }
    public void setFinalArrivalDate(String finalArrivalDate) { this.finalArrivalDate = finalArrivalDate; }

    public String getStatusOfShipment() { return statusOfShipment; }
    public void setStatusOfShipment(String statusOfShipment) { this.statusOfShipment = statusOfShipment; }
}
