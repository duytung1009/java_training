package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;

import java.math.BigDecimal;

/**
 * Tính phí ship theo cả đơn (không cộng {@code calculateShipping} từng dòng).
 */
public final class ShippingFeeCalculator {

    private ShippingFeeCalculator() {
    }

    /**
     * Phí = {@code ShippingZone.of(address).feeFor(tổng cân nặng)}, scale 2. Tổng cân nặng = Σ weightKg × quantity
     * của các dòng {@code PhysicalProduct}; dòng {@code BundleProduct} cộng weightKg các item physical bên trong
     * × quantity của dòng (bundle lồng bundle thì tính đệ quy). {@code baseFee} chỉ tính một lần cho cả đơn.
     *
     * @throws IllegalArgumentException khi order / address null, hoặc đơn không có hàng vật lý nào
     */
    public static BigDecimal feeFor(Order order, Address address) {
        throw new UnsupportedOperationException("TODO S07-extra");
    }
}
