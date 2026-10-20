package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tính doanh thu từ danh sách product. Không dùng instanceof: dựa vào polymorphism của calculateFinalPrice().
 */
public class PricingEngine {

    /**
     * @return Σ calculateFinalPrice(), scale 2; list rỗng → 0.00
     * @throws IllegalArgumentException khi products null hoặc chứa null
     */
    public BigDecimal calculateRevenue(List<Product> products) {
        throw new UnsupportedOperationException("TODO S03");
    }
}
