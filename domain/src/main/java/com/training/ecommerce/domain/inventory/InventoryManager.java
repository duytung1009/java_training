package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Quản lý catalog và tồn kho. Catalog lưu bằng {@code Map<id, Product>} (giữ thứ tự thêm), tra theo id O(1).
 */
public class InventoryManager {
    private final Map<String, Product> catalog = new LinkedHashMap<>();

    /**
     * @throws IllegalArgumentException khi product null hoặc trùng id với product đã có
     */
    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (catalog.containsKey(product.getId())) {
            throw new IllegalArgumentException("Trùng id: " + product.getId());
        }
        catalog.put(product.getId(), product);
    }

    /** S05: product có id tương ứng; không có (kể cả id null) → {@code Optional.empty()}. */
    public Optional<Product> findById(String id) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * S05: lọc catalog theo điều kiện, giữ thứ tự thêm; list trả về là snapshot.
     *
     * @throws IllegalArgumentException khi criteria null
     */
    public List<Product> search(Predicate<Product> criteria) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * Trừ kho cho toàn bộ order theo kiểu all-or-nothing: cộng dồn số lượng theo product,
     * kiểm tra đủ hàng cho mọi product rồi mới trừ.
     *
     * @throws IllegalArgumentException khi order null
     * @throws ProductNotFoundException khi order có product không nằm trong kho
     * @throws OutOfStockException      khi không đủ hàng; kho giữ nguyên
     */
    public void decreaseStock(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        Map<Product, Integer> required = new LinkedHashMap<>();
        for (OrderLine line : order.getLines()) {
            Product stored = catalog.get(line.getProduct().getId());
            if (stored == null) {
                throw new ProductNotFoundException(line.getProduct().getId());
            }
            required.merge(stored, line.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Product, Integer> entry : required.entrySet()) {
            Product product = entry.getKey();
            if (entry.getValue() > product.getStock()) {
                throw new OutOfStockException(product.getId(), entry.getValue(), product.getStock());
            }
        }
        for (Map.Entry<Product, Integer> entry : required.entrySet()) {
            entry.getKey().decreaseStock(entry.getValue());
        }
    }
}
