package com.training.ecommerce.domain.shipping;

/**
 * Trạng thái của vận đơn. Các trạng thái kết thúc: DELIVERED, RETURNED, CANCELLED.
 */
public enum ShipmentStatus {
    CREATED, IN_TRANSIT, DELIVERED, RETURNING, RETURNED, CANCELLED;

    /**
     * @return true nếu là trạng thái kết thúc (DELIVERED, RETURNED, CANCELLED), không chuyển đi đâu được nữa
     */
    public boolean isTerminal() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }
}
