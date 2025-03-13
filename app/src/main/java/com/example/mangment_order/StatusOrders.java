package com.example.mangment_order;

public enum StatusOrders {
    Opened(0),
    Ready_For_Shipment(1),
    Shipped(2),
    Arrive_To_Destination(3),
    Received(4),
    Cancelled(5); // New status

    private int pos;
    StatusOrders(int pos){
        this.pos = pos;
    }
    public static StatusOrders getStatusBypos(int pos){
        switch (pos){
            case 0: return Opened;
            case 1: return Ready_For_Shipment;
            case 2: return Shipped;
            case 3: return Arrive_To_Destination;
            case 4: return Received;
            case 5: return Cancelled;
            default: return null;
        }
    }
    public String getStatus(){
        return name();
    }
    @Override
    public String toString() {
        return name();
    }
}
