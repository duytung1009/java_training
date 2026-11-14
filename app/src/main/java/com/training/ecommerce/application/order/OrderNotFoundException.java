package com.training.ecommerce.application.order;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Không tìm thấy đơn hàng theo id (NOT_FOUND, mã {@code ORDER_NOT_FOUND}).
 */
public class OrderNotFoundException extends BusinessException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super(Category.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
