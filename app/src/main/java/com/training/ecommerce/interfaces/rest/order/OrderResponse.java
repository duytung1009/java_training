package com.training.ecommerce.interfaces.rest.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Đơn hàng trả về cho client; mọi số tiền scale 2.
 */
public record OrderResponse(
        String id,
        BigDecimal discountRate,
        BigDecimal taxRate,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal finalAmount) {

    /** unitPrice là snapshot giá lúc đặt. */
    public record Line(String productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
