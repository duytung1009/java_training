package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Map;
import java.util.TreeMap;

/**
 * {@link StockStore} trên PostgreSQL. Mỗi product trừ bằng một câu lệnh nguyên tử
 * {@code UPDATE ... SET stock = stock - :q WHERE id = :id AND stock >= :q}: không đọc-rồi-ghi nên nhiều
 * transaction cùng trừ một product vẫn không oversell (dòng bị khóa tới khi transaction giữ nó kết thúc,
 * transaction sau đánh giá lại điều kiện với số tồn mới). CHECK {@code stock >= 0} ở schema là lớp chặn cuối.
 *
 * <p>All-or-nothing giữa các line KHÔNG do class này bảo đảm mà do transaction của caller (ví dụ
 * {@code @Transactional} ở {@code OrderService}): khi một line ném exception, transaction rollback và các line đã trừ
 * được hoàn lại. Gọi ngoài transaction thì mỗi câu UPDATE tự commit và line trước đó sẽ không được hoàn.
 *
 * <p>Số lượng được cộng dồn theo product ({@code Order.quantitiesByProductId}) và trừ theo thứ tự id tăng dần để hai đơn đặt cùng bộ product theo
 * thứ tự khác nhau không khóa chéo (deadlock).
 */
public class JdbcStockStore implements StockStore {
    private final JdbcClient jdbc;

    public JdbcStockStore(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc không được null");
        }
        this.jdbc = jdbc;
    }

    @Override
    public void decreaseStock(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        for (Map.Entry<String, Integer> entry : new TreeMap<>(order.quantitiesByProductId()).entrySet()) {
            decrease(entry.getKey(), entry.getValue());
        }
    }

    private void decrease(String productId, int quantity) {
        int updated = jdbc.sql("UPDATE products SET stock = stock - :q WHERE id = :id AND stock >= :q")
                .param("q", quantity)
                .param("id", productId)
                .update();
        if (updated == 0) {
            int available = jdbc.sql("SELECT stock FROM products WHERE id = :id")
                    .param("id", productId)
                    .query(Integer.class)
                    .optional()
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            throw new OutOfStockException(productId, quantity, available);
        }
    }
}
