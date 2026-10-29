package com.training.ecommerce.domain.shipping;

/**
 * Chuyển trạng thái vận đơn không có trong sơ đồ. Kế thừa IllegalStateException như {@code OutOfStockException}.
 */
public class IllegalShipmentTransitionException extends IllegalStateException {
    private final ShipmentStatus current;
    private final String action;

    public IllegalShipmentTransitionException(String action, ShipmentStatus current, ShipmentStatus expected) {
        super("Không thể " + action + " khi vận đơn ở trạng thái " + current + " (cần " + expected + ")");
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
