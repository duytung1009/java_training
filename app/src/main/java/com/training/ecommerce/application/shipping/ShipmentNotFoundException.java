package com.training.ecommerce.application.shipping;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Không tìm thấy vận đơn theo id (NOT_FOUND, mã {@code SHIPMENT_NOT_FOUND}).
 */
public class ShipmentNotFoundException extends BusinessException {
    private final String shipmentId;

    public ShipmentNotFoundException(String shipmentId) {
        super(Category.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Không tìm thấy vận đơn: " + shipmentId);
        this.shipmentId = shipmentId;
    }

    public String getShipmentId() {
        return shipmentId;
    }
}
