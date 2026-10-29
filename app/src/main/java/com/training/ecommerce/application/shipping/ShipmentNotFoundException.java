package com.training.ecommerce.application.shipping;

/**
 * Không tìm thấy vận đơn theo id.
 */
public class ShipmentNotFoundException extends RuntimeException {
    private final String shipmentId;

    public ShipmentNotFoundException(String shipmentId) {
        super("Không tìm thấy vận đơn: " + shipmentId);
        this.shipmentId = shipmentId;
    }

    public String getShipmentId() {
        return shipmentId;
    }
}
