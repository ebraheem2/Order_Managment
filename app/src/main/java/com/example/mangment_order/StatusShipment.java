package com.example.mangment_order;

public enum StatusShipment {
    PENDING("The Shipment is Pending",0),
    LOCAL_SHIPPING_COMPANY("Shipment is with the local shipping company.",1),
    WAREHOUSE_IN_ORIGIN("Shipment is in the warehouse of the country of origin.",2),
    RECEIVED_BY_AIRLINE("Shipment has been received by the airline for transportation.",3),
    LEFT_COUNTRY_OF_ORIGIN("Shipment has left the country of origin.",4),
    WAREHOUSE_IN_DESTINATION("Shipment is in the warehouse of the destination country.",5),
    ARRIVED_AT_POST_OFFICE("Shipment arrived at the post office of the country of destination.",6),
    OUT_FOR_DELIVERY("Shipment is out for delivery.",7),
    DELIVERED("Shipment has been delivered successfully.",8);

    private String statusMessage;
    private int pos;


    StatusShipment(String s,int pos) {
        this.statusMessage=s;
        this.pos=pos;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
    public static StatusShipment getStatusMessageByPostion(int pos){
        switch (pos){
            case 0:
                return StatusShipment.PENDING;
            case 1:
                return StatusShipment.LOCAL_SHIPPING_COMPANY;
            case 2:
                return StatusShipment.WAREHOUSE_IN_ORIGIN;
            case 3:
                return StatusShipment.RECEIVED_BY_AIRLINE;
            case 4:
                return StatusShipment.LEFT_COUNTRY_OF_ORIGIN;
            case 5:
                return StatusShipment.WAREHOUSE_IN_DESTINATION;
            case 6:
                return StatusShipment.ARRIVED_AT_POST_OFFICE;
            case 7:
                return StatusShipment.OUT_FOR_DELIVERY;
            case 8:
                return StatusShipment.DELIVERED;
            default:
                return null;
        }

    }
    @Override
    public String toString(){
        return name();
    }
}
