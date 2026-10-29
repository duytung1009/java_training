package com.training.ecommerce.application.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;

/**
 * Port trừ / hoàn tồn kho cho một đơn. Mỗi implementation tự bảo đảm không oversell trong môi trường của nó
 * (lock trong bộ nhớ, hoặc câu UPDATE có điều kiện ở database).
 */
public interface StockStore {

    /**
     * Trừ kho cho mọi line của order theo kiểu all-or-nothing.
     *
     * @throws IllegalArgumentException khi order null
     * @throws ProductNotFoundException khi order có product không tồn tại
     * @throws OutOfStockException      khi không đủ hàng
     */
    void decreaseStock(Order order);

    /**
     * Hoàn kho cho mọi line của order (đối xứng {@link #decreaseStock}) theo kiểu all-or-nothing.
     *
     * @throws IllegalArgumentException khi order null
     * @throws ProductNotFoundException khi order có product không tồn tại
     */
    void increaseStock(Order order);
}
