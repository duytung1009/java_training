package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Không đủ hàng tồn kho (CONFLICT, mã {@code OUT_OF_STOCK}).
 */
public class OutOfStockException extends BusinessException {
    private final String productId;
    private final int requested;
    private final int available;

    public OutOfStockException(String productId, int requested, int available) {
        super(Category.CONFLICT, "OUT_OF_STOCK", "Không đủ hàng cho " + productId + ": còn " + available + ", cần " + requested);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
