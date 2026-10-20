package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;

/**
 * Mã giảm giá (value object): mã và tỉ lệ giảm trong (0, 1].
 */
public final class DiscountCode {
    private final String code;
    private final BigDecimal rate;

    public DiscountCode(String code, BigDecimal rate) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code không được rỗng");
        }
        if (rate == null || rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rate phải trong (0, 1]");
        }
        this.code = code;
        this.rate = rate;
    }

    public String getCode() { return code; }
    public BigDecimal getRate() { return rate; }
}
