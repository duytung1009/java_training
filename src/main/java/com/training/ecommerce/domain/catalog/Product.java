package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;

/**
 * Sản phẩm trong catalog: id, tên, đơn giá và số lượng tồn kho.
 */
public class Product {

    /**
     * Tạo product với đầy đủ thông tin.
     *
     * @throws IllegalArgumentException khi id/name null hoặc blank, price null hoặc âm, stock âm
     */
    public Product(String id, String name, BigDecimal price, int stock) {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * Tạo product chưa có hàng tồn (stock = 0). Phải dùng constructor chaining.
     *
     * @throws IllegalArgumentException cùng quy tắc như constructor đầy đủ
     */
    public Product(String id, String name, BigDecimal price) {
        throw new UnsupportedOperationException("TODO S02");
    }

    public String getId() {
        throw new UnsupportedOperationException("TODO S02");
    }

    public String getName() {
        throw new UnsupportedOperationException("TODO S02");
    }

    public BigDecimal getPrice() {
        throw new UnsupportedOperationException("TODO S02");
    }

    public int getStock() {
        throw new UnsupportedOperationException("TODO S02");
    }
}
