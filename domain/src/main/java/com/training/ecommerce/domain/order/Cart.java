package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Giỏ hàng: product → số lượng, giữ thứ tự thêm. Không trừ kho (việc của InventoryManager).
 */
public class Cart {
    private final Map<Product, Integer> items = new LinkedHashMap<>();

    /**
     * Thêm (cộng dồn nếu đã có).
     *
     * @throws IllegalArgumentException khi product null hoặc quantity &lt;= 0
     * @throws OutOfStockException      khi tổng số lượng trong giỏ vượt tồn kho (giỏ giữ nguyên)
     */
    public void add(Product product, int quantity) {
        requireProduct(product);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        ensureInStock(product, getQuantity(product) + quantity);
        items.merge(product, quantity, Integer::sum);
    }

    /**
     * Đặt lại số lượng; 0 thì bỏ khỏi giỏ.
     *
     * @throws IllegalArgumentException khi product null hoặc quantity &lt; 0
     * @throws ProductNotFoundException khi product chưa có trong giỏ
     * @throws OutOfStockException      khi quantity vượt tồn kho (giỏ giữ nguyên)
     */
    public void changeQuantity(Product product, int quantity) {
        requireProduct(product);
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity phải >= 0");
        }
        if (!items.containsKey(product)) {
            throw new ProductNotFoundException(product.getId());
        }
        if (quantity == 0) {
            items.remove(product);
            return;
        }
        ensureInStock(product, quantity);
        items.put(product, quantity);
    }

    /** Bỏ product khỏi giỏ; chưa có thì không làm gì. */
    public void remove(Product product) {
        items.remove(product);
    }

    /** @return số lượng trong giỏ, 0 nếu chưa có */
    public int getQuantity(Product product) {
        return items.getOrDefault(product, 0);
    }

    /** @return product → số lượng theo thứ tự thêm; view không sửa được */
    public Map<Product, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Tạo Order: mỗi product một OrderLine theo thứ tự thêm.
     *
     * @throws IllegalStateException khi giỏ rỗng
     */
    public Order toOrder(BigDecimal discountRate, BigDecimal taxRate) {
        if (items.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng rỗng");
        }
        Order order = new Order(discountRate, taxRate);
        for (Map.Entry<Product, Integer> item : items.entrySet()) {
            order.addLine(new OrderLine(item.getKey(), item.getValue()));
        }
        return order;
    }

    private static void requireProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
    }

    private static void ensureInStock(Product product, int totalQuantity) {
        if (totalQuantity > product.getStock()) {
            throw new OutOfStockException(product.getId(), totalQuantity, product.getStock());
        }
    }
}
