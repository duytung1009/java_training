package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.Discountable;
import com.training.ecommerce.domain.shipping.Shippable;
import com.training.ecommerce.domain.shipping.ShippingZone;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sản phẩm vật lý: có cân nặng, giao hàng được và áp dụng được mã giảm giá.
 */
public class PhysicalProduct extends Product implements Shippable, Discountable {
    private final BigDecimal weightKg;

    public PhysicalProduct(String id, String name, BigDecimal basePrice, int stock, BigDecimal weightKg) {
        this(id, name, basePrice, stock, weightKg, Category.OTHER);
    }

    /** S05: kèm category. */
    public PhysicalProduct(String id, String name, BigDecimal basePrice, int stock, BigDecimal weightKg, Category category) {
        super(id, name, basePrice, stock, category);
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new IllegalArgumentException("weightKg phải > 0");
        }
        this.weightKg = weightKg;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        return getBasePrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateShipping(Address address) {
        return ShippingZone.of(address).feeFor(weightKg);
    }
}
