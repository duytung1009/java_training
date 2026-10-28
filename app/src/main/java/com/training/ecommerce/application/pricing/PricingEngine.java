package com.training.ecommerce.application.pricing;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.pricing.PricingRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Tính doanh thu từ danh sách product. Không dùng instanceof: dựa vào polymorphism của calculateFinalPrice().
 */
public class PricingEngine {

    public BigDecimal calculateRevenue(List<Product> products) {
        // Bản S03 = áp rule identity: không lặp lại logic
        return calculateRevenue(products, PricingRule.identity());
    }

    /**
     * S05: Tổng rule.apply(calculateFinalPrice()) của từng product, scale 2; list rỗng → 0.00.
     *
     * @throws IllegalArgumentException khi products/rule null hoặc products chứa null
     */
    public BigDecimal calculateRevenue(List<Product> products, PricingRule rule) {
        if (products == null || rule == null) {
            throw new IllegalArgumentException("products và rule không được null");
        }
        // Không dùng products.contains(null): List.of(...).contains(null) ném NPE
        if (products.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("products không được chứa null");
        }
        return products.stream()
                .map(Product::calculateFinalPrice)
                .map(rule::apply)                            // rule áp lên TỪNG product, rồi mới cộng
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
