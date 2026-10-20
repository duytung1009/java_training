package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tính doanh thu từ danh sách product. Không dùng instanceof: dựa vào polymorphism của calculateFinalPrice().
 */
public class PricingEngine {

    public BigDecimal calculateRevenue(List<Product> products) {
        if (products == null) {
            throw new IllegalArgumentException("products không được null");
        }
        BigDecimal revenue = BigDecimal.ZERO;
        for (Product product : products) {
            if (product == null) {
                throw new IllegalArgumentException("products không được chứa null");
            }
            revenue = revenue.add(product.calculateFinalPrice());
        }
        return revenue.setScale(2, RoundingMode.HALF_UP);
    }
}
