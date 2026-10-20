package com.training.ecommerce.domain.order;

import java.math.BigDecimal;

/**
 * Đơn hàng: subtotal → discount → tax → finalAmount. Mọi số tiền trả về có scale 2, làm tròn HALF_UP.
 */
public class Order {

    /**
     * @param discountRate tỉ lệ giảm giá trong [0, 1], ví dụ 0.10 = 10%
     * @param taxRate      tỉ lệ thuế &gt;= 0, ví dụ 0.08 = 8%
     * @throws IllegalArgumentException khi rate null hoặc ngoài khoảng cho phép
     */
    public Order(BigDecimal discountRate, BigDecimal taxRate) {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * @throws IllegalArgumentException khi line null
     */
    public void addLine(OrderLine line) {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * @return bản copy các line theo thứ tự add; sửa mảng trả về không ảnh hưởng Order
     */
    public OrderLine[] getLines() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /** Σ lineTotal. */
    public BigDecimal subtotal() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /** subtotal × discountRate. */
    public BigDecimal discountAmount() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /** (subtotal − discountAmount) × taxRate. */
    public BigDecimal taxAmount() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /** subtotal − discountAmount + taxAmount. */
    public BigDecimal finalAmount() {
        throw new UnsupportedOperationException("TODO S02");
    }
}
