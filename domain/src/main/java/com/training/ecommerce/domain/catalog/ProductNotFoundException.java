package com.training.ecommerce.domain.catalog;

/**
 * Không tìm thấy product theo id. Kế thừa IllegalArgumentException để tương thích code cũ.
 */
public class ProductNotFoundException extends IllegalArgumentException {
    private final String productId;

    public ProductNotFoundException(String productId) {
        super("Không tìm thấy product: " + productId);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
