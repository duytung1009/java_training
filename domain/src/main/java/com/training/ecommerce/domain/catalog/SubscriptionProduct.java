package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gói thuê bao: giá theo tháng × số tháng. getBasePrice() là monthlyPrice.
 */
public class SubscriptionProduct extends Product {
    private final int months;

    public SubscriptionProduct(String id, String name, BigDecimal monthlyPrice, int months, int stock) {
        super(id, name, monthlyPrice, stock);
        if (months < 1) {
            throw new IllegalArgumentException("months phải >= 1");
        }
        this.months = months;
    }

    public int getMonths() {
        return months;
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        return getBasePrice().multiply(BigDecimal.valueOf(months)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
