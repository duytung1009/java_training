package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.pricing.Discountable;

import java.math.BigDecimal;

/**
 * Sản phẩm số: áp dụng được mã giảm giá, không giao hàng (không Shippable).
 */
public class DigitalProduct extends Product implements Discountable {

    public DigitalProduct(String id, String name, BigDecimal basePrice, int stock) {
        super(id, name, basePrice, stock);
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        throw new UnsupportedOperationException("TODO S03");
    }

    @Override
    public BigDecimal applyDiscount(DiscountCode code) {
        throw new UnsupportedOperationException("TODO S03");
    }
}
