package com.training.ecommerce.domain.pricing;

import java.time.LocalDate;

// Value object
public class DiscountCode {
    public final String code;
    public final double discountRate;
    public final LocalDate expiredAt;

    public DiscountCode(String code, double discountRate, LocalDate expiredAt) {
        this.code = code;
        this.discountRate = discountRate;
        this.expiredAt = expiredAt;
    }
}
