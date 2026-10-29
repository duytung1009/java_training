package com.training.ecommerce.application.shipping;

/**
 * Đơn đã có vận đơn còn hiệu lực (khác CANCELLED / RETURNED) nên không tạo thêm được.
 */
public class ActiveShipmentExistsException extends IllegalStateException {
    private final String orderId;

    public ActiveShipmentExistsException(String orderId) {
        super("Đơn " + orderId + " đã có vận đơn còn hiệu lực");
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
