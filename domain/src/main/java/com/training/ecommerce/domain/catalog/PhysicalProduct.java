package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.pricing.Discountable;
import com.training.ecommerce.domain.shipping.Shippable;

import java.math.BigDecimal;

/**
 * Sản phẩm vật lý: có cân nặng, giao hàng được và áp dụng được mã giảm giá.
 */
public class PhysicalProduct extends Product implements Shippable, Discountable {

    /**
     * @throws IllegalArgumentException theo quy tắc của Product, hoặc khi weightKg null hoặc &lt;= 0
     */
    public PhysicalProduct(String id, String name, BigDecimal basePrice, int stock, BigDecimal weightKg) {
        super(id, name, basePrice, stock);
        // TODO S03: kiểm tra và lưu weightKg
    }

    public BigDecimal getWeightKg() {
        throw new UnsupportedOperationException("TODO S03");
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        throw new UnsupportedOperationException("TODO S03");
    }

    @Override
    public BigDecimal applyDiscount(DiscountCode code) {
        throw new UnsupportedOperationException("TODO S03");
    }

    @Override
    public BigDecimal calculateShipping(Address address) {
        throw new UnsupportedOperationException("TODO S03");
    }
}
