package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Chuyển trạng thái vận đơn không có trong sơ đồ (CONFLICT, mã {@code ILLEGAL_SHIPMENT_TRANSITION}).
 */
public class IllegalShipmentTransitionException extends BusinessException {
    private final ShipmentStatus current;
    private final String action;

    public IllegalShipmentTransitionException(String action, ShipmentStatus current, ShipmentStatus expected) {
        super(Category.CONFLICT, "ILLEGAL_SHIPMENT_TRANSITION", "Không thể " + action + " khi vận đơn ở trạng thái " + current + " (cần " + expected + ")");
        this.action = action;
        this.current = current;
    }

    public ShipmentStatus getCurrent() {
        return current;
    }

    public String getAction() {
        return action;
    }
}
