package com.training.ecommerce.domain.catalog;

/**
 * Không đủ hàng tồn kho. Kế thừa IllegalStateException để tương thích code cũ.
 */
public class OutOfStockException extends IllegalStateException {

    public OutOfStockException(String productId, int requested, int available) {
        super("TODO S04"); // TODO S04: message có productId, requested, available; lưu các giá trị
    }

    public String getProductId() {
        throw new UnsupportedOperationException("TODO S04");
    }

    public int getRequested() {
        throw new UnsupportedOperationException("TODO S04");
    }

    public int getAvailable() {
        throw new UnsupportedOperationException("TODO S04");
    }
}
