package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Quản lý catalog và tồn kho. Catalog lưu bằng {@code Map<id, Product>} (giữ thứ tự thêm), tra theo id O(1).
 *
 * <p>S06: an toàn khi nhiều thread cùng gọi. Mọi method public đều {@code synchronized} trên chính object này
 * (một lock chung), nên:
 * <ul>
 *   <li>{@code decreaseStock}: bước kiểm tra đủ hàng và bước trừ nằm trong cùng một lock → không oversell,
 *       all-or-nothing vẫn đúng khi nhiều đơn chạy song song;</li>
 *   <li>{@code addProduct}: kiểm tra trùng id rồi {@code put} là một bước, không thread nào chen vào giữa;</li>
 *   <li>{@code search} / {@code findById}: đọc trong lock nên không gặp {@code ConcurrentModificationException}
 *       khi thread khác đang thêm product, và luôn thấy giá trị mới nhất (visibility).</li>
 * </ul>
 * Đánh đổi: mọi checkout xếp hàng nối tiếp, kể cả các đơn không liên quan. Đủ đúng cho domain in-memory;
 * cần nhanh hơn thì đo trước, rồi mới cân nhắc lock theo từng product (phải khóa theo thứ tự id để tránh deadlock).
 */
public class InventoryManager {
    private final Map<String, Product> catalog = new LinkedHashMap<>();

    /**
     * @throws IllegalArgumentException khi product null hoặc trùng id với product đã có
     */
    public synchronized void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (catalog.containsKey(product.getId())) {
            throw new IllegalArgumentException("Trùng id: " + product.getId());
        }
        catalog.put(product.getId(), product);
    }

    /** S05: product có id tương ứng; không có (kể cả id null) → {@code Optional.empty()}. */
    public synchronized Optional<Product> findById(String id) {
        // Map.get(null) của LinkedHashMap trả null, nhưng chặn rõ để không phụ thuộc kiểu Map
        return id == null ? Optional.empty() : Optional.ofNullable(catalog.get(id));
    }

    /**
     * S05: lọc catalog theo điều kiện, giữ thứ tự thêm; list trả về là snapshot.
     *
     * @throws IllegalArgumentException khi criteria null
     */
    public synchronized List<Product> search(Predicate<Product> criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria không được null");
        }
        return catalog.values().stream()
                .filter(criteria)
                .toList();                                   // Java 16+: list mới, không sửa được
    }

    /**
     * Trừ kho cho toàn bộ order theo kiểu all-or-nothing: cộng dồn số lượng theo product,
     * kiểm tra đủ hàng cho mọi product rồi mới trừ.
     *
     * @throws IllegalArgumentException khi order null
     * @throws ProductNotFoundException khi order có product không nằm trong kho
     * @throws OutOfStockException      khi không đủ hàng; kho giữ nguyên
     */
    public synchronized void decreaseStock(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        // Bước 1: cộng dồn số lượng theo product trong kho (toMap có merge Integer::sum, giữ thứ tự)
        Map<Product, Integer> required = Arrays.stream(order.getLines())
                .collect(Collectors.toMap(
                        line -> findStored(line.getProduct().getId()),
                        OrderLine::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new));
        // Bước 2: kiểm tra hết trước khi trừ (all-or-nothing)
        required.entrySet().stream()
                .filter(entry -> entry.getValue() > entry.getKey().getStock())
                .findFirst()
                .ifPresent(entry -> {
                    throw new OutOfStockException(entry.getKey().getId(), entry.getValue(), entry.getKey().getStock());
                });
        // Bước 3: đủ hàng mới trừ. Đây là side effect (sửa tồn kho) nên dùng vòng for cho rõ ý, không dùng forEach
        for (Map.Entry<Product, Integer> entry : required.entrySet()) {
            entry.getKey().decreaseStock(entry.getValue());
        }
    }

    private Product findStored(String id) {
        return findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
