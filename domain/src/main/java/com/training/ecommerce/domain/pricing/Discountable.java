package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sản phẩm áp dụng được mã giảm giá. Cách tính dùng chung nằm ở default method.
 */
public interface Discountable {

    /** Giá bán trước khi giảm (Product đã có sẵn method này). */
    BigDecimal calculateFinalPrice();

    /**
     * @return giá sau giảm: calculateFinalPrice() × (1 − rate), scale 2, HALF_UP
     * @throws IllegalArgumentException khi code null
     */
    default BigDecimal applyDiscount(DiscountCode code) {
        if (code == null) {
            throw new IllegalArgumentException("code không được null");
        }
        return calculateFinalPrice().multiply(BigDecimal.ONE.subtract(code.getRate())).setScale(2, RoundingMode.HALF_UP);
    }
}
