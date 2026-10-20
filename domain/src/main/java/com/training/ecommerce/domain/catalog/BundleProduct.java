package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;

/**
 * Combo nhiều sản phẩm (có thể chứa bundle khác), bán với tỉ lệ giảm riêng.
 * Thêm class này mà không phải sửa PricingEngine (OCP).
 */
public class BundleProduct extends Product {

    /**
     * @param discountRate tỉ lệ giảm của combo trong [0, 1]
     * @throws IllegalArgumentException theo quy tắc của Product, hoặc khi discountRate null/ngoài [0, 1],
     *                                  items null/rỗng hoặc chứa null
     */
    public BundleProduct(String id, String name, int stock, BigDecimal discountRate, Product... items) {
        super(id, name, BigDecimal.ZERO, stock); // TODO S03: basePrice của bundle (ngoài spec), kiểm tra + lưu items
    }

    /** Σ calculateFinalPrice() của items × (1 − discountRate), scale 2, HALF_UP. */
    @Override
    public BigDecimal calculateFinalPrice() {
        throw new UnsupportedOperationException("TODO S03");
    }
}
