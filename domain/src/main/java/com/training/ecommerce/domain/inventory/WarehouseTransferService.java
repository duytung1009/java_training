package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;

/**
 * S06: chuyển hàng giữa hai kho, an toàn khi nhiều thread chuyển theo hai chiều cùng lúc (không deadlock).
 *
 * <p>Giữ lock của cả hai kho trong suốt lần chuyển, nên không ai thấy trạng thái "đã trừ bên này, chưa cộng bên kia".
 * Hai lock luôn được lấy theo cùng một thứ tự (kho có id nhỏ hơn trước, bất kể chiều chuyển) → phá điều kiện
 * circular wait, nên transfer(A, B) và transfer(B, A) chạy song song cũng không deadlock.
 */
public class WarehouseTransferService {

    /**
     * Chuyển quantity của productId từ kho from sang kho to.
     *
     * @throws IllegalArgumentException khi tham số null, quantity &lt;= 0, from == to hoặc hai kho trùng id
     * @throws OutOfStockException      khi kho from không đủ hàng; cả hai kho giữ nguyên
     */
    public void transfer(Warehouse from, Warehouse to, String productId, int quantity) {
        if (from == null || to == null || productId == null) {
            throw new IllegalArgumentException("from, to, productId không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        // Cùng id thì không xác định được thứ tự khóa; chuyển cho chính mình cũng là bug của caller
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Không chuyển hàng trong cùng một kho: " + from.getId());
        }

        Warehouse first = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Warehouse second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                from.remove(productId, quantity);   // thiếu hàng thì ném ở đây, chưa có gì thay đổi
                to.add(productId, quantity);        // quantity > 0 đã kiểm, bước này không thể lỗi
            }
        }
    }
}
