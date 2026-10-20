package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;

/**
 * Mã giảm giá (value object): mã và tỉ lệ giảm trong (0, 1].
 */
public class DiscountCode {

    /**
     * @throws IllegalArgumentException khi code null/blank, rate null hoặc ngoài (0, 1]
     */
    public DiscountCode(String code, BigDecimal rate) {
        throw new UnsupportedOperationException("TODO S03");
    }

    public String getCode() {
        throw new UnsupportedOperationException("TODO S03");
    }

    public BigDecimal getRate() {
        throw new UnsupportedOperationException("TODO S03");
    }
}
