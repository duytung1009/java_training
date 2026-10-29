package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.shared.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryStockStoreTest {
    private Repository<Product, String> products;
    private InMemoryStockStore store;
    private Product a;
    private Product b;

    @BeforeEach
    void setUp() {
        products = new InMemoryRepository<>();
        store = new InMemoryStockStore(products);
        a = products.save(new PhysicalProduct("A", "A", new BigDecimal("100"), 10, BigDecimal.ONE));
        b = products.save(new PhysicalProduct("B", "B", new BigDecimal("100"), 5, BigDecimal.ONE));
    }

    private static Order order(OrderLine... lines) {
        Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        for (OrderLine line : lines) {
            order.addLine(line);
        }
        return order;
    }

    private static OrderLine line(Product product, int quantity) {
        return new OrderLine(product, quantity);
    }

    @Test
    void decreasesEveryLine() {
        store.decreaseStock(order(line(a, 3), line(b, 5)));

        assertEquals(7, a.getStock());
        assertEquals(0, b.getStock());
    }

    @Test
    void allOrNothingWhenLaterLineLacksStock() {
        Order order = order(line(a, 3), line(b, 5));
        b.decreaseStock(1);     // hàng bị mua mất sau khi đơn được dựng

        assertThrows(OutOfStockException.class, () -> store.decreaseStock(order));

        assertEquals(10, a.getStock());
        assertEquals(4, b.getStock());
    }

    @Test
    void sumsDuplicateLines() {
        assertThrows(OutOfStockException.class, () -> store.decreaseStock(order(line(a, 6), line(a, 6))));
        assertEquals(10, a.getStock());
    }

    @Test
    void unknownProductThrowsAndNothingChanges() {
        Product ghost = new PhysicalProduct("GHOST", "G", BigDecimal.TEN, 5, BigDecimal.ONE);

        assertThrows(ProductNotFoundException.class, () -> store.decreaseStock(order(line(a, 1), line(ghost, 1))));

        assertEquals(10, a.getStock());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> store.decreaseStock(null));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryStockStore(null));
    }

    @Test
    void increasesEveryLineAndSumsDuplicates() {
        store.increaseStock(order(line(a, 3), line(b, 2), line(a, 2)));

        assertEquals(15, a.getStock());
        assertEquals(7, b.getStock());
    }

    @Test
    void increaseUnknownProductThrowsAndNothingChanges() {
        Product ghost = new PhysicalProduct("GHOST", "G", BigDecimal.TEN, 5, BigDecimal.ONE);

        assertThrows(ProductNotFoundException.class, () -> store.increaseStock(order(line(a, 1), line(ghost, 1))));

        assertEquals(10, a.getStock());
    }

    @Test
    void increaseRejectsNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> store.increaseStock(null));
    }
}
