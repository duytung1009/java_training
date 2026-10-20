package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;

/**
 * Gói thuê bao: giá theo tháng × số tháng. getBasePrice() là monthlyPrice.
 */
public class SubscriptionProduct extends Product {

    /**
     * @throws IllegalArgumentException theo quy tắc của Product, hoặc khi months &lt; 1
     */
    public SubscriptionProduct(String id, String name, BigDecimal monthlyPrice, int months, int stock) {
        super(id, name, monthlyPrice, stock);
        // TODO S03: kiểm tra và lưu months
    }

    public int getMonths() {
        throw new UnsupportedOperationException("TODO S03");
    }

    /** monthlyPrice × months, scale 2, HALF_UP. */
    @Override
    public BigDecimal calculateFinalPrice() {
        throw new UnsupportedOperationException("TODO S03");
    }
}
