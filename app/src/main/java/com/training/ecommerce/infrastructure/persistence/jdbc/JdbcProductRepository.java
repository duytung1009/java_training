package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductType;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * {@link Repository} cho product trên PostgreSQL, dùng {@link JdbcClient}. Không gắn stereotype Spring:
 * được khai báo bean trong {@code JdbcPersistenceConfig}.
 *
 * <ul>
 *   <li>Một bảng {@code products} cho mọi loại, cột {@code type} quyết định subtype khi đọc.</li>
 *   <li>{@code save} là upsert ({@code ON CONFLICT DO UPDATE}) nên giữ {@code seq} lần thêm đầu → thứ tự thêm không đổi.</li>
 *   <li>Bundle: item lưu ở {@code bundle_items} theo position, mỗi lần save được thay mới. Item phải đã có trong
 *       bảng products (khóa ngoại), nếu không {@code save} ném {@code DataIntegrityViolationException}.</li>
 *   <li>Atomicity của nhiều câu lệnh trong một {@code save} do transaction của caller bảo đảm.</li>
 * </ul>
 */
public class JdbcProductRepository implements Repository<Product, String> {
    private static final String COLUMNS = "id, type, name, base_price, stock, category, weight_kg, months, discount_rate";

    private final JdbcClient jdbc;

    public JdbcProductRepository(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc không được null");
        }
        this.jdbc = jdbc;
    }

    /**
     * @throws IllegalArgumentException khi product null hoặc subtype chưa được hỗ trợ
     */
    @Override
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("entity không được null");
        }
        ProductType type = ProductType.of(product);
        BigDecimal weightKg = product instanceof PhysicalProduct p ? p.getWeightKg() : null;
        Integer months = product instanceof SubscriptionProduct s ? s.getMonths() : null;
        BigDecimal discountRate = product instanceof BundleProduct b ? b.getDiscountRate() : null;

        jdbc.sql("""
                        INSERT INTO products (id, type, name, base_price, stock, category, weight_kg, months, discount_rate)
                        VALUES (:id, :type, :name, :basePrice, :stock, :category, :weightKg, :months, :discountRate)
                        ON CONFLICT (id) DO UPDATE SET
                            type = EXCLUDED.type, name = EXCLUDED.name, base_price = EXCLUDED.base_price,
                            stock = EXCLUDED.stock, category = EXCLUDED.category, weight_kg = EXCLUDED.weight_kg,
                            months = EXCLUDED.months, discount_rate = EXCLUDED.discount_rate
                        """)
                .param("id", product.getId())
                .param("type", type.name())
                .param("name", product.getName())
                .param("basePrice", product.getBasePrice())
                .param("stock", product.getStock())
                .param("category", product.getCategory().name())
                .param("weightKg", weightKg, Types.NUMERIC)
                .param("months", months, Types.INTEGER)
                .param("discountRate", discountRate, Types.NUMERIC)
                .update();

        jdbc.sql("DELETE FROM bundle_items WHERE bundle_id = :id").param("id", product.getId()).update();
        if (product instanceof BundleProduct bundle) {
            Product[] items = bundle.getItems();
            for (int position = 0; position < items.length; position++) {
                jdbc.sql("INSERT INTO bundle_items (bundle_id, item_id, position) VALUES (:bundle, :item, :position)")
                        .param("bundle", bundle.getId())
                        .param("item", items[position].getId())
                        .param("position", position)
                        .update();
            }
        }
        return product;
    }

    @Override
    public Optional<Product> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.sql("SELECT " + COLUMNS + " FROM products WHERE id = :id")
                .param("id", id)
                .query(JdbcProductRepository::readRow)
                .optional()
                .map(this::toProduct);
    }

    /** Thứ tự theo {@code seq} (thứ tự thêm); list không sửa được. */
    @Override
    public List<Product> findAll() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM products ORDER BY seq")
                .query(JdbcProductRepository::readRow)
                .list()
                .stream()
                .map(this::toProduct)
                .toList();
    }

    /**
     * @throws org.springframework.dao.DataIntegrityViolationException khi product đang là item của bundle khác
     *                                                                 hoặc đã nằm trong một đơn hàng
     */
    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return jdbc.sql("DELETE FROM products WHERE id = :id").param("id", id).update() > 0;
    }

    @Override
    public int count() {
        return jdbc.sql("SELECT count(*) FROM products").query(Integer.class).single();
    }

    // ---- mapping ----

    /** Dữ liệu thô của một dòng; tách khỏi bước dựng Product để không chạy query lồng khi ResultSet còn mở. */
    private record Row(String id, String type, String name, BigDecimal basePrice, int stock, Category category,
                       BigDecimal weightKg, Integer months, BigDecimal discountRate) {
    }

    private static Row readRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        int months = rs.getInt("months");
        Integer monthsOrNull = rs.wasNull() ? null : months;
        return new Row(rs.getString("id"), rs.getString("type"), rs.getString("name"), rs.getBigDecimal("base_price"),
                rs.getInt("stock"), Category.valueOf(rs.getString("category")), rs.getBigDecimal("weight_kg"),
                monthsOrNull, rs.getBigDecimal("discount_rate"));
    }

    private Product toProduct(Row r) {
        return switch (ProductType.valueOf(r.type())) {
            case PHYSICAL -> new PhysicalProduct(r.id(), r.name(), r.basePrice(), r.stock(), r.weightKg(), r.category());
            case DIGITAL -> new DigitalProduct(r.id(), r.name(), r.basePrice(), r.stock(), r.category());
            case SUBSCRIPTION ->
                    new SubscriptionProduct(r.id(), r.name(), r.basePrice(), r.months(), r.stock(), r.category());
            case BUNDLE -> new BundleProduct(r.id(), r.name(), r.stock(), r.discountRate(), r.category(),
                    loadItems(r.id()).toArray(Product[]::new));
        };
    }

    private List<Product> loadItems(String bundleId) {
        List<String> itemIds = jdbc.sql("SELECT item_id FROM bundle_items WHERE bundle_id = :id ORDER BY position")
                .param("id", bundleId)
                .query(String.class)
                .list();
        return itemIds.stream()
                .map(itemId -> findById(itemId).orElseThrow(
                        () -> new IllegalStateException("Bundle " + bundleId + " trỏ tới item không tồn tại: " + itemId)))
                .toList();
    }
}
