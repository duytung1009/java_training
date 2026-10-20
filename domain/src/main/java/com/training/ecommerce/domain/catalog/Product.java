package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;

/**
 * Sản phẩm trong catalog: id, tên, đơn giá và số lượng tồn kho.
 */
public class Product {
    private final String id;
    private final String name;
    private final BigDecimal price;
    private int stock;

    public Product(String id, String name, BigDecimal price) {
        this(id, name, price, 0);
    }

    public Product(String id, String name, BigDecimal price, int stock) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id không được rỗng");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name không được rỗng");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price phải >= 0");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock phải >= 0");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /**
     * Giảm tồn kho. Chỉ InventoryManager nên gọi method này.
     *
     * @throws IllegalArgumentException khi quantity &lt;= 0
     * @throws IllegalStateException    khi quantity lớn hơn tồn kho hiện tại
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (quantity > stock) {
            throw new IllegalStateException("Không đủ hàng cho " + id + ": còn " + stock + ", cần " + quantity);
        }
        stock -= quantity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
}
