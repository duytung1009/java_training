package com.training.ecommerce.domain.catalog;

/**
 * Không đủ hàng tồn kho. Kế thừa IllegalStateException để tương thích code cũ.
 */
public class OutOfStockException extends IllegalStateException {
    private final String productId;
    private final int requested;
    private final int available;

    public OutOfStockException(String productId, int requested, int available) {
        super("Không đủ hàng cho " + productId + ": còn " + available + ", cần " + requested);
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
