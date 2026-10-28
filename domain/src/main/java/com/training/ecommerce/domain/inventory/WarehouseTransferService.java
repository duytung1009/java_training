package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;

/**
 * S06: chuyển hàng giữa hai kho, an toàn khi nhiều thread chuyển theo hai chiều cùng lúc (không deadlock).
 */
public class WarehouseTransferService {

    /**
     * Chuyển quantity của productId từ kho from sang kho to. Không ai thấy trạng thái "đã trừ bên này, chưa cộng bên kia".
     *
     * @throws IllegalArgumentException khi tham số null, quantity &lt;= 0, from == to hoặc hai kho trùng id
     * @throws OutOfStockException      khi kho from không đủ hàng; cả hai kho giữ nguyên
     */
    public void transfer(Warehouse from, Warehouse to, String productId, int quantity) {
        throw new UnsupportedOperationException("TODO S06");
    }
}
