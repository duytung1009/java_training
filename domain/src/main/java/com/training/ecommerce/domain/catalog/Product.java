package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.shared.repository.Identifiable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Sản phẩm trong catalog. Mỗi loại sản phẩm tự quyết định giá bán qua {@link #calculateFinalPrice()}.
 * Hai product bằng nhau khi cùng id, bất kể là subtype nào.
 */
public abstract class Product implements Identifiable<String> {
    protected static final int MONEY_SCALE = 2;

    private final String id;
    private final String name;
    private final BigDecimal basePrice;
    private int stock;

    protected Product(String id, String name, BigDecimal basePrice) {
        this(id, name, basePrice, 0);
    }

    protected Product(String id, String name, BigDecimal basePrice, int stock) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id không được rỗng");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name không được rỗng");
        }
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalArgumentException("basePrice phải >= 0");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock phải >= 0");
        }
        this.id = id;
        this.name = name;
        this.basePrice = basePrice;
        this.stock = stock;
    }

    /** Giá bán theo loại sản phẩm, chưa gồm giảm giá và phí ship; scale 2, HALF_UP. */
    public abstract BigDecimal calculateFinalPrice();

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

    @Override
    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getStock() { return stock; }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product product)) {
            return false;
        }
        return id.equals(product.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name=" + name + ", basePrice=" + basePrice + "}";
    }
}
