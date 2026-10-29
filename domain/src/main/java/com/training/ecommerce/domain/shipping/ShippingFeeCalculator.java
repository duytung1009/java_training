package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

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
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        if (address == null) {
            throw new IllegalArgumentException("address không được null");
        }
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (OrderLine line : order.getLines()) {
            totalWeight = totalWeight.add(weightOf(line.getProduct()).multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        if (totalWeight.signum() == 0) {
            throw new IllegalArgumentException("Đơn không có hàng vật lý để tính phí ship");
        }
        return ShippingZone.of(address).feeFor(totalWeight);
    }

    /** Cân nặng của một đơn vị product: physical = weightKg, bundle = tổng các item (đệ quy), loại khác = 0. */
    private static BigDecimal weightOf(Product product) {
        return switch (product) {
            case PhysicalProduct physical -> physical.getWeightKg();
            case BundleProduct bundle -> {
                BigDecimal total = BigDecimal.ZERO;
                for (Product item : bundle.getItems()) {
                    total = total.add(weightOf(item));
                }
                yield total;
            }
            default -> BigDecimal.ZERO;
        };
    }
}
