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

    /** S05: kèm category. */
    public SubscriptionProduct(String id, String name, BigDecimal monthlyPrice, int months, int stock, Category category) {
        this(id, name, monthlyPrice, months, stock);
        // TODO S05: constructor này chưa truyền được category lên Product (đang gọi this(...) bản cũ).
        //  Gợi ý: cho constructor cũ gọi this(..., Category.OTHER), còn constructor này gọi super(..., category).
    }

    public int getMonths() {
        return months;
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        return getBasePrice().multiply(BigDecimal.valueOf(months)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
