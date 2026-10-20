package com.training.ecommerce.domain.catalog;

/**
 * Không tìm thấy product theo id. Kế thừa IllegalArgumentException để tương thích code cũ.
 */
public class ProductNotFoundException extends IllegalArgumentException {

    public ProductNotFoundException(String productId) {
        super("TODO S04"); // TODO S04: message có productId, lưu productId
    }

    public String getProductId() {
        throw new UnsupportedOperationException("TODO S04");
    }
}
