package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.shared.repository.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link StockStore} in-memory trên {@link Repository} của product. Cùng logic với
 * {@code InventoryManager.decreaseStock}: cộng dồn theo product, kiểm tra hết rồi mới trừ (all-or-nothing).
 * Method {@code synchronized} nên bước kiểm tra và bước trừ không bị chen ngang bởi thread khác của store này.
 */
public class InMemoryStockStore implements StockStore {
    private final Repository<Product, String> products;

    public InMemoryStockStore(Repository<Product, String> products) {
        if (products == null) {
            throw new IllegalArgumentException("products không được null");
        }
        this.products = products;
    }

    @Override
    public synchronized void decreaseStock(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        Map<Product, Integer> required = new LinkedHashMap<>();
        order.quantitiesByProductId().forEach((id, quantity) ->
                required.put(products.findById(id).orElseThrow(() -> new ProductNotFoundException(id)), quantity));
        for (Map.Entry<Product, Integer> entry : required.entrySet()) {
            if (entry.getValue() > entry.getKey().getStock()) {
                throw new OutOfStockException(entry.getKey().getId(), entry.getValue(), entry.getKey().getStock());
            }
        }
        for (Map.Entry<Product, Integer> entry : required.entrySet()) {
            entry.getKey().decreaseStock(entry.getValue());
        }
    }
}
