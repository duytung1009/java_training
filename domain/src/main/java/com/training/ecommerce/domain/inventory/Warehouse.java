package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;

import java.util.HashMap;
import java.util.Map;

/**
 * S06: kho hàng có id, giữ tồn kho theo product id. An toàn khi nhiều thread cùng gọi.
 *
 * <p>Mọi method đọc/ghi tồn kho đều {@code synchronized} trên chính object {@code Warehouse}. Đây cũng là lock mà
 * {@link WarehouseTransferService} dùng khi chuyển hàng, nên một lần chuyển kho và một lần nhập/xuất trực tiếp
 * không bao giờ chen vào giữa nhau.
 *
 * <p>S07-extra: {@link InventoryManager#replenish} khóa hub trước rồi mới khóa kho. Vì vậy khi đang giữ lock
 * của kho thì không được gọi vào {@code InventoryManager}.
 */
public class Warehouse {
    private final String id;                                   // final + gán trong constructor → thread nào cũng thấy đúng
    private final Map<String, Integer> stock = new HashMap<>(); // chỉ đụng tới khi đang giữ lock của this

    /**
     * @throws IllegalArgumentException khi id null hoặc rỗng
     */
    public Warehouse(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id kho không được rỗng");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Tồn kho hiện tại của product; product chưa có trong kho → 0.
     *
     * @throws IllegalArgumentException khi productId null
     */
    public synchronized int stockOf(String productId) {
        checkProductId(productId);
        return stock.getOrDefault(productId, 0);
    }

    /**
     * Nhập thêm hàng (cộng dồn).
     *
     * @throws IllegalArgumentException khi productId null hoặc quantity &lt;= 0
     */
    public synchronized void add(String productId, int quantity) {
        checkProductId(productId);
        checkQuantity(quantity);
        stock.merge(productId, quantity, Integer::sum);
    }

    /**
     * Xuất hàng. Kiểm tra và trừ nằm trong cùng một lock (không check-then-act hở).
     *
     * @throws IllegalArgumentException khi productId null hoặc quantity &lt;= 0
     * @throws OutOfStockException      khi quantity lớn hơn tồn kho; kho giữ nguyên
     */
    public synchronized void remove(String productId, int quantity) {
        checkProductId(productId);
        checkQuantity(quantity);
        int available = stock.getOrDefault(productId, 0);
        if (quantity > available) {
            throw new OutOfStockException(productId, quantity, available);
        }
        stock.put(productId, available - quantity);
    }

    @Override
    public String toString() {
        return "Warehouse[" + id + "]";
    }

    private static void checkProductId(String productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId không được null");
        }
    }

    private static void checkQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
    }
}
