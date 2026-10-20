package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.Arrays;

/**
 * Quản lý catalog và tồn kho. Catalog lưu bằng {@code Product[]}, tự nới khi đầy.
 */
public class InventoryManager {
    private static final int INITIAL_CAPACITY = 10;

    private Product[] catalog = new Product[INITIAL_CAPACITY];
    private int count = 0;

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (indexOf(product.getId()) >= 0) {
            throw new IllegalArgumentException("Trùng id: " + product.getId());
        }
        if (count == catalog.length) {
            catalog = Arrays.copyOf(catalog, catalog.length * 2);
        }
        catalog[count++] = product;
    }

    /** @return product có id tương ứng, hoặc {@code null} nếu không có. */
    public Product findById(String id) {
        int index = indexOf(id);
        return index >= 0 ? catalog[index] : null;
    }

    /**
     * Trừ kho cho toàn bộ order theo kiểu all-or-nothing: kiểm tra đủ hàng cho mọi line
     * (cộng dồn các line cùng product) rồi mới trừ.
     *
     * @throws IllegalArgumentException khi order null hoặc có product không nằm trong kho
     * @throws IllegalStateException    khi không đủ hàng; kho giữ nguyên
     */
    public void decreaseStock(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        int[] required = new int[count];
        for (OrderLine line : order.getLines()) {
            String productId = line.getProduct().getId();
            int index = indexOf(productId);
            if (index < 0) {
                throw new IllegalArgumentException("Product không có trong kho: " + productId);
            }
            required[index] += line.getQuantity();
        }
        for (int i = 0; i < count; i++) {
            if (required[i] > catalog[i].getStock()) {
                throw new IllegalStateException("Không đủ hàng cho " + catalog[i].getId()
                        + ": còn " + catalog[i].getStock() + ", cần " + required[i]);
            }
        }
        for (int i = 0; i < count; i++) {
            if (required[i] > 0) {
                catalog[i].decreaseStock(required[i]);
            }
        }
    }

    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            if (catalog[i].getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
