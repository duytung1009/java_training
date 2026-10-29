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
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }

    /**
     * Vận đơn ở trạng thái này có chặn tạo vận đơn mới cho cùng đơn không: chỉ CANCELLED và RETURNED là "hết hiệu lực";
     * DELIVERED tuy đã kết thúc nhưng vẫn chặn (đơn đã giao xong không giao lại). Khác {@link #isTerminal()}.
     */
    public boolean blocksNewShipment() {
        return this != CANCELLED && this != RETURNED;
    }
}
