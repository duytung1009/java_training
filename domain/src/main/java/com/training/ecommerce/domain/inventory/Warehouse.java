package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;

/**
 * S06: kho hàng có id, giữ tồn kho theo product id. An toàn khi nhiều thread cùng gọi.
 */
public class Warehouse {

    /**
     * @throws IllegalArgumentException khi id null hoặc rỗng
     */
    public Warehouse(String id) {
        throw new UnsupportedOperationException("TODO S06");
    }

    public String getId() {
        throw new UnsupportedOperationException("TODO S06");
    }

    /**
     * Tồn kho hiện tại của product; product chưa có trong kho → 0.
     *
     * @throws IllegalArgumentException khi productId null
     */
    public int stockOf(String productId) {
        throw new UnsupportedOperationException("TODO S06");
    }

    /**
     * Nhập thêm hàng (cộng dồn).
     *
     * @throws IllegalArgumentException khi productId null hoặc quantity &lt;= 0
     */
    public void add(String productId, int quantity) {
        throw new UnsupportedOperationException("TODO S06");
    }

    /**
     * Xuất hàng.
     *
     * @throws IllegalArgumentException khi productId null hoặc quantity &lt;= 0
     * @throws OutOfStockException      khi quantity lớn hơn tồn kho; kho giữ nguyên
     */
    public void remove(String productId, int quantity) {
        throw new UnsupportedOperationException("TODO S06");
    }
}
