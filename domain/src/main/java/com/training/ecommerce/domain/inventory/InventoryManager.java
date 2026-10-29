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
 *
 * <p>S07-extra: {@code replenish} nhập hàng từ {@link Warehouse} lên hub, giữ lock của hub rồi tới lock của kho.
 * Thứ tự khóa toàn cục là <b>hub trước, kho sau</b>: không code nào được giữ lock một kho rồi gọi vào hub,
 * nếu không sẽ có vòng chờ với {@code replenish}.
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

    /**
     * Nhập quantity của productId từ kho from lên hub, all-or-nothing: không ai thấy trạng thái
     * "kho đã trừ, hub chưa cộng".
     *
     * @throws IllegalArgumentException khi from / productId null hoặc quantity &lt;= 0
     * @throws ProductNotFoundException khi hub chưa có product này; kho giữ nguyên
     * @throws OutOfStockException      khi kho from không đủ hàng; hai bên giữ nguyên
     */
    public synchronized void replenish(Warehouse from, String productId, int quantity) {
        if (from == null || productId == null) {
            throw new IllegalArgumentException("from, productId không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        Product product = findStored(productId);    // hub chưa có thì ném ở đây, kho chưa bị đụng tới
        synchronized (from) {                        // thứ tự khóa: hub (this) trước, kho sau
            from.remove(productId, quantity);        // thiếu hàng thì ném ở đây, chưa có gì thay đổi
            product.increaseStock(quantity);         // quantity > 0 đã kiểm, bước này không thể lỗi
        }
    }

    /**
     * Hoàn kho cho toàn bộ order (đối xứng {@link #decreaseStock}): cộng lại số lượng theo
     * {@code order.quantitiesByProductId()}, all-or-nothing.
     *
     * @throws IllegalArgumentException khi order null
     * @throws ProductNotFoundException khi hub thiếu product nào đó; không product nào được cộng
     */
    public synchronized void restock(Order order) {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    private Product findStored(String id) {
        return findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
