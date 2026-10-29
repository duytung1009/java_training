package com.training.ecommerce.application.shipping;

import com.training.ecommerce.application.order.OrderNotFoundException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.domain.shipping.IllegalShipmentTransitionException;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShipmentStatus;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryRepository;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryShipmentRepository;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryStockStore;
import com.training.ecommerce.shared.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipmentServiceTest {
    private static final Address HANOI = new Address("1 Phố Huế", "Hà Nội");

    private Repository<Product, String> products;
    private Repository<Order, String> orders;
    private ShipmentRepository shipments;
    private ShipmentService service;
    private Product tshirt;
    private Product jeans;
    private Order order;

    @BeforeEach
    void setUp() {
        products = new InMemoryRepository<>();
        orders = new InMemoryRepository<>();
        shipments = new InMemoryShipmentRepository();
        service = new ShipmentService(orders, shipments, new InMemoryStockStore(products));
        tshirt = products.save(new PhysicalProduct("A", "Áo", new BigDecimal("100000"), 10, new BigDecimal("0.5")));
        jeans = products.save(new PhysicalProduct("B", "Quần", new BigDecimal("200000"), 10, BigDecimal.ONE));
        // đơn đã chốt: áo × 2 + quần × 1, kho đã trừ (còn áo 8, quần 9)
        order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        order.addLine(new OrderLine(tshirt, 2));
        order.addLine(new OrderLine(jeans, 1));
        tshirt.decreaseStock(2);
        jeans.decreaseStock(1);
        orders.save(order);
    }

    private Shipment newShipment() {
        return service.create(order.getId(), HANOI);
    }

    private Shipment inTransit() {
        Shipment shipment = newShipment();
        return service.pickUp(shipment.getId());
    }

    @Test
    void createComputesFeeForWholeOrder() {
        Shipment shipment = newShipment();

        assertEquals(new BigDecimal("30000.00"), shipment.getFee());      // 20000 + 5000 × (0.5×2 + 1)
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        assertEquals(order.getId(), shipment.getOrderId());
        assertEquals(shipment, service.findById(shipment.getId()));
        assertEquals(1, service.findByOrderId(order.getId()).size());
    }

    @Test
    void createForUnknownOrderThrows() {
        assertThrows(OrderNotFoundException.class, () -> service.create("nope", HANOI));
    }

    @Test
    void createRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null, HANOI));
        assertThrows(IllegalArgumentException.class, () -> service.create(order.getId(), null));
    }

    @Test
    void unknownShipmentThrowsNotFound() {
        assertThrows(ShipmentNotFoundException.class, () -> service.findById("nope"));
        assertThrows(ShipmentNotFoundException.class, () -> service.pickUp("nope"));
        assertThrows(ShipmentNotFoundException.class, () -> service.cancel("nope"));
        assertThrows(ShipmentNotFoundException.class, () -> service.markReturned(null));
    }

    @Test
    void rejectsSecondActiveShipment() {
        inTransit();

        assertThrows(ActiveShipmentExistsException.class, () -> service.create(order.getId(), HANOI));
        assertEquals(1, shipments.count());
    }

    @Test
    void rejectsAfterDelivered() {
        Shipment shipment = inTransit();
        service.deliver(shipment.getId());

        assertThrows(ActiveShipmentExistsException.class, () -> service.create(order.getId(), HANOI));
    }

    @Test
    void allowsNewAfterCancelled() {
        Shipment first = newShipment();
        service.cancel(first.getId());

        Shipment second = newShipment();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, service.findByOrderId(order.getId()).size());
    }

    @Test
    void allowsNewAfterReturned() {
        Shipment first = inTransit();
        service.refuse(first.getId(), "Khách từ chối");
        service.markReturned(first.getId());

        Shipment second = newShipment();

        assertEquals(ShipmentStatus.CREATED, second.getStatus());
        assertEquals(2, service.findByOrderId(order.getId()).size());
    }

    @Test
    void returnRestocks() {
        Shipment shipment = inTransit();
        service.refuse(shipment.getId(), "Khách từ chối");
        assertEquals(8, tshirt.getStock());

        Shipment returned = service.markReturned(shipment.getId());

        assertEquals(ShipmentStatus.RETURNED, returned.getStatus());
        assertEquals(10, tshirt.getStock());
        assertEquals(10, jeans.getStock());
    }

    @Test
    void failingThreeTimesThenReturnRestocks() {
        Shipment shipment = inTransit();
        service.failAttempt(shipment.getId(), "1");
        service.failAttempt(shipment.getId(), "2");
        Shipment third = service.failAttempt(shipment.getId(), "3");
        assertEquals(ShipmentStatus.RETURNING, third.getStatus());
        assertEquals(8, tshirt.getStock());

        service.markReturned(shipment.getId());

        assertEquals(10, tshirt.getStock());
    }

    @Test
    void cancelRestocks() {
        Shipment shipment = newShipment();

        Shipment cancelled = service.cancel(shipment.getId());

        assertEquals(ShipmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(10, tshirt.getStock());
        assertEquals(10, jeans.getStock());
    }

    @Test
    void deliverDoesNotRestock() {
        Shipment shipment = inTransit();

        Shipment delivered = service.deliver(shipment.getId());

        assertEquals(ShipmentStatus.DELIVERED, delivered.getStatus());
        assertEquals(8, tshirt.getStock());
        assertEquals(9, jeans.getStock());
    }

    @Test
    void invalidTransitionLeavesStockUntouched() {
        Shipment shipment = inTransit();

        assertThrows(IllegalShipmentTransitionException.class, () -> service.cancel(shipment.getId()));       // đã lấy hàng
        assertThrows(IllegalShipmentTransitionException.class, () -> service.markReturned(shipment.getId())); // chưa RETURNING

        assertEquals(ShipmentStatus.IN_TRANSIT, service.findById(shipment.getId()).getStatus());
        assertEquals(8, tshirt.getStock());
        assertEquals(9, jeans.getStock());
    }

    @Test
    void blankReasonIsRejected() {
        Shipment shipment = inTransit();

        assertThrows(IllegalArgumentException.class, () -> service.failAttempt(shipment.getId(), " "));
        assertThrows(IllegalArgumentException.class, () -> service.refuse(shipment.getId(), null));
        assertEquals(0, service.findById(shipment.getId()).getFailedAttempts());
    }

    @Test
    void rejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new ShipmentService(null, shipments, new InMemoryStockStore(products)));
        assertThrows(IllegalArgumentException.class, () -> new ShipmentService(orders, null, new InMemoryStockStore(products)));
        assertThrows(IllegalArgumentException.class, () -> new ShipmentService(orders, shipments, null));
    }
}
