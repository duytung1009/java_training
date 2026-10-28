package com.training.ecommerce.application.order;

/**
 * Không tìm thấy đơn hàng theo id.
 */
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Không tìm thấy đơn hàng: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
