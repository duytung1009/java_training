package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link Repository} cho đơn hàng trên PostgreSQL. Không gắn stereotype Spring (khai báo bean trong config).
 *
 * <p>{@code save} là upsert: ghi đè tỉ lệ của đơn cùng id (giữ {@code seq} và {@code created_at}), xóa các line cũ rồi
 * ghi lại. Đơn giá của line là snapshot trong {@code order_lines.unit_price}, đọc lên bằng constructor khôi phục
 * của {@link OrderLine} nên không bị ảnh hưởng khi giá product đổi sau này. Nhiều câu lệnh trong một {@code save}
 * cần chạy trong transaction của caller để all-or-nothing.
 */
public class JdbcOrderRepository implements Repository<Order, String> {
    private static final String SELECT_ORDERS = "SELECT id, discount_rate, tax_rate FROM orders";

    private record LineRow(String productId, int quantity, BigDecimal unitPrice) {
    }

    private final JdbcClient jdbc;
    private final Repository<Product, String> products;

    public JdbcOrderRepository(JdbcClient jdbc, Repository<Product, String> products) {
        if (jdbc == null || products == null) {
            throw new IllegalArgumentException("jdbc và products không được null");
        }
        this.jdbc = jdbc;
        this.products = products;
    }

    /**
     * @throws IllegalArgumentException khi order null
     */
    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("entity không được null");
        }
        jdbc.sql("""
                        INSERT INTO orders (id, discount_rate, tax_rate) VALUES (:id, :discountRate, :taxRate)
                        ON CONFLICT (id) DO UPDATE SET discount_rate = EXCLUDED.discount_rate, tax_rate = EXCLUDED.tax_rate
                        """)
                .param("id", order.getId())
                .param("discountRate", order.getDiscountRate())
                .param("taxRate", order.getTaxRate())
                .update();
        jdbc.sql("DELETE FROM order_lines WHERE order_id = :id").param("id", order.getId()).update();
        OrderLine[] lines = order.getLines();
        for (int i = 0; i < lines.length; i++) {
            jdbc.sql("""
                            INSERT INTO order_lines (order_id, line_no, product_id, quantity, unit_price)
                            VALUES (:orderId, :lineNo, :productId, :quantity, :unitPrice)
                            """)
                    .param("orderId", order.getId())
                    .param("lineNo", i)
                    .param("productId", lines[i].getProduct().getId())
                    .param("quantity", lines[i].getQuantity())
                    .param("unitPrice", lines[i].getUnitPrice())
                    .update();
        }
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.sql(SELECT_ORDERS + " WHERE id = :id")
                .param("id", id)
                .query(JdbcOrderRepository::readHeader)
                .optional()
                .map(order -> withLines(order, new HashMap<>()));
    }

    /**
     * Thứ tự theo {@code seq}; list không sửa được. Mỗi product chỉ được tải một lần cho cả lời gọi, dù nằm trong
     * nhiều đơn.
     */
    @Override
    public List<Order> findAll() {
        Map<String, Product> cache = new HashMap<>();
        return jdbc.sql(SELECT_ORDERS + " ORDER BY seq")
                .query(JdbcOrderRepository::readHeader)
                .list()
                .stream()
                .map(order -> withLines(order, cache))
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return jdbc.sql("DELETE FROM orders WHERE id = :id").param("id", id).update() > 0;   // line xóa theo CASCADE
    }

    @Override
    public int count() {
        return jdbc.sql("SELECT count(*) FROM orders").query(Integer.class).single();
    }

    /** Dựng Order chưa có line; line được thêm sau khi ResultSet đã đóng để không chạy query lồng. */
    private static Order readHeader(ResultSet rs, int rowNum) throws SQLException {
        return new Order(rs.getString("id"), rs.getBigDecimal("discount_rate"), rs.getBigDecimal("tax_rate"));
    }

    private Order withLines(Order order, Map<String, Product> productCache) {
        List<LineRow> lines = jdbc.sql("""
                        SELECT product_id, quantity, unit_price FROM order_lines WHERE order_id = :id ORDER BY line_no
                        """)
                .param("id", order.getId())
                .query((rs, n) -> new LineRow(rs.getString("product_id"), rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price")))
                .list();
        for (LineRow line : lines) {
            Product product = productCache.computeIfAbsent(line.productId(), id -> products.findById(id).orElseThrow(
                    () -> new IllegalStateException("Đơn " + order.getId() + " trỏ tới product không tồn tại: " + id)));
            order.addLine(new OrderLine(product, line.quantity(), line.unitPrice()));
        }
        return order;
    }
}
