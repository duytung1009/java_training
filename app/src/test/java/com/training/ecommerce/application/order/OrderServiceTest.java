package com.training.ecommerce.application.order;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryRepository;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryStockStore;
import com.training.ecommerce.shared.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {
    private Repository<Product, String> products;
    private Repository<Order, String> orders;
    private OrderService service;

    @BeforeEach
    void setUp() {
        products = new InMemoryRepository<>();
        orders = new InMemoryRepository<>();
        service = new OrderService(products, orders, new InMemoryStockStore(products));
        products.save(new PhysicalProduct("A", "A", new BigDecimal("100000"), 10, BigDecimal.ONE));
        products.save(new PhysicalProduct("B", "B", new BigDecimal("50000"), 2, BigDecimal.ONE));
    }

    private static PlaceOrderCommand command(PlaceOrderCommand.Line... lines) {
        return new PlaceOrderCommand(new BigDecimal("0.1"), new BigDecimal("0.08"), List.of(lines));
    }

    @Test
    void placeOrderSavesOrderAndDecreasesStock() {
        Order order = service.placeOrder(command(new PlaceOrderCommand.Line("A", 3), new PlaceOrderCommand.Line("B", 2)));

        assertSame(order, service.findById(order.getId()));
        assertEquals(2, order.getLines().length);
        assertEquals(new BigDecimal("400000.00"), order.subtotal());
        assertEquals(7, products.findById("A").orElseThrow().getStock());
        assertEquals(0, products.findById("B").orElseThrow().getStock());
        assertEquals(1, service.findAll().size());
    }

    @Test
    void unknownProductThrowsAndNothingChanges() {
        assertThrows(ProductNotFoundException.class,
                () -> service.placeOrder(command(new PlaceOrderCommand.Line("A", 1), new PlaceOrderCommand.Line("X", 1))));

        assertEquals(10, products.findById("A").orElseThrow().getStock());
        assertEquals(0, orders.count());
    }

    @Test
    void outOfStockThrowsAndNothingChanges() {
        assertThrows(OutOfStockException.class,
                () -> service.placeOrder(command(new PlaceOrderCommand.Line("A", 1), new PlaceOrderCommand.Line("B", 3))));

        assertEquals(10, products.findById("A").orElseThrow().getStock());
        assertEquals(0, orders.count());
    }

    @Test
    void duplicateLinesAreCheckedTogether() {
        assertThrows(OutOfStockException.class,
                () -> service.placeOrder(command(new PlaceOrderCommand.Line("B", 2), new PlaceOrderCommand.Line("B", 2))));

        assertEquals(2, products.findById("B").orElseThrow().getStock());
    }

    @Test
    void findMissingOrderThrows() {
        OrderNotFoundException e = assertThrows(OrderNotFoundException.class, () -> service.findById("nope"));
        assertEquals("nope", e.getOrderId());
        assertThrows(OrderNotFoundException.class, () -> service.findById(null));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.placeOrder(null));
        assertThrows(IllegalArgumentException.class, () -> new OrderService(null, orders, new InMemoryStockStore(products)));
        assertThrows(IllegalArgumentException.class, () -> new OrderService(products, null, new InMemoryStockStore(products)));
        assertThrows(IllegalArgumentException.class, () -> new OrderService(products, orders, null));
    }

    @Test
    void invalidRatesAreRejectedByOrderWhenPlacing() {
        List<PlaceOrderCommand.Line> one = List.of(new PlaceOrderCommand.Line("A", 1));

        assertThrows(IllegalArgumentException.class, () -> service.placeOrder(new PlaceOrderCommand(null, BigDecimal.ZERO, one)));
        assertThrows(IllegalArgumentException.class,
                () -> service.placeOrder(new PlaceOrderCommand(new BigDecimal("1.5"), BigDecimal.ZERO, one)));
        assertThrows(IllegalArgumentException.class,
                () -> service.placeOrder(new PlaceOrderCommand(BigDecimal.ZERO, new BigDecimal("-1"), one)));
        assertEquals(10, products.findById("A").orElseThrow().getStock());
        assertEquals(0, orders.count());
    }

    @Test
    void commandValidation() {
        List<PlaceOrderCommand.Line> one = List.of(new PlaceOrderCommand.Line("A", 1));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand(BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand(BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand(BigDecimal.ZERO, BigDecimal.ZERO,
                java.util.Arrays.asList((PlaceOrderCommand.Line) null)));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line("A", 0));
    }

    @Test
    void commandCopiesLinesDefensively() {
        List<PlaceOrderCommand.Line> mutable = new ArrayList<>(List.of(new PlaceOrderCommand.Line("A", 1)));
        PlaceOrderCommand command = new PlaceOrderCommand(BigDecimal.ZERO, BigDecimal.ZERO, mutable);

        mutable.add(new PlaceOrderCommand.Line("B", 1));

        assertEquals(1, command.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> command.lines().add(new PlaceOrderCommand.Line("B", 1)));
    }
}
