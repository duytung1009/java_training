package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;

/**
 * Sản phẩm áp dụng được mã giảm giá.
 */
public interface Discountable {

    /**
     * @return giá sau giảm: calculateFinalPrice() × (1 − rate), scale 2, HALF_UP
     * @throws IllegalArgumentException khi code null
     */
    BigDecimal applyDiscount(DiscountCode code);
}
