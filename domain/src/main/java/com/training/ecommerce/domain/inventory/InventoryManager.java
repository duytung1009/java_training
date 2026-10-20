package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.Product;

/**
 * Quản lý catalog và tồn kho. Catalog lưu bằng {@code Product[]} (không dùng ArrayList).
 */
public class InventoryManager {

    public InventoryManager() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * Thêm product vào catalog; mảng tự nới khi đầy.
     *
     * @throws IllegalArgumentException khi product null hoặc trùng id với product đã có
     */
    public void addProduct(Product product) {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * Tìm product theo id.
     *
     * @return đúng instance đã add, hoặc {@code null} nếu không có (kể cả khi id null)
     */
    public Product findById(String id) {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * Giảm tồn kho của product.
     *
     * @throws IllegalArgumentException khi id không tồn tại/null hoặc quantity &lt;= 0
     * @throws IllegalStateException    khi quantity lớn hơn tồn kho hiện tại (stock giữ nguyên)
     */
    public void decreaseStock(String productId, int quantity) {
        throw new UnsupportedOperationException("TODO S02");
    }
}
