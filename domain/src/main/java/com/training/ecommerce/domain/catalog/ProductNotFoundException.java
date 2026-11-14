package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Không tìm thấy product theo id (NOT_FOUND, mã {@code PRODUCT_NOT_FOUND}).
 */
public class ProductNotFoundException extends BusinessException {
    private final String productId;

    public ProductNotFoundException(String productId) {
        super(Category.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy product: " + productId);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
