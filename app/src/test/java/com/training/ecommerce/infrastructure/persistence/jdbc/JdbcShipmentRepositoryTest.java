package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShipmentEvent;
import com.training.ecommerce.domain.shipping.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcShipmentRepositoryTest extends PostgresJdbcTestSupport {
    private static final Address HANOI = new Address("1 Phố Huế", "Hà Nội");

    private JdbcProductRepository products;
    private JdbcOrderRepository orders;
    private JdbcShipmentRepository shipments;
    private Product tshirt;

    @BeforeEach
    void setUpRepos() {
        products = new JdbcProductRepository(jdbc);
        orders = new JdbcOrderRepository(jdbc, products);
        shipments = new JdbcShipmentRepository(jdbc);
        tshirt = products.save(new PhysicalProduct("P1", "Áo", new BigDecimal("150000"), 50, new BigDecimal("0.5")));
        orders.save(order("O1"));
        orders.save(order("O2"));
    }

    private Order order(String id) {
        Order order = new Order(id, BigDecimal.ZERO, BigDecimal.ZERO);
        order.addLine(new OrderLine(tshirt, 2));
        return order;
    }

    private Shipment shipment(String orderId) {
        return new Shipment(orderId, HANOI, new BigDecimal("30000"));
    }

    @Test
    void roundTripKeepsFieldsHistoryAndAttempts() {
        Shipment shipment = shipment("O1");
        shipment.pickUp();
        shipment.failAttempt("Không có người nhận");
        shipment.failAttempt("Sai địa chỉ");
        shipments.save(shipment);

        Shipment found = shipments.findById(shipment.getId()).orElseThrow();

        assertThat(found.getOrderId()).isEqualTo("O1");
        assertThat(found.getAddress().getStreet()).isEqualTo("1 Phố Huế");
        assertThat(found.getAddress().getCity()).isEqualTo("Hà Nội");
        assertThat(found.getFee()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(found.getTrackingCode()).isEqualTo(shipment.getTrackingCode());
        assertThat(found.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(found.getFailedAttempts()).isEqualTo(2);
        assertThat(found.history()).isEqualTo(shipment.history());
        assertThat(found.history().get(3)).isEqualTo(new ShipmentEvent(ShipmentStatus.IN_TRANSIT, "Sai địa chỉ"));
    }

    @Test
    void saveTwiceUpdatesAndRestoredShipmentContinuesLifecycle() {
        Shipment shipment = shipments.save(shipment("O1"));
        shipment.pickUp();
        shipments.save(shipment);

        Shipment found = shipments.findById(shipment.getId()).orElseThrow();
        found.refuse("Từ chối");
        shipments.save(found);

        Shipment reloaded = shipments.findById(shipment.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ShipmentStatus.RETURNING);
        assertThat(reloaded.history()).hasSize(3);
        assertThat(shipments.count()).isEqualTo(1);
    }

    @Test
    void findMissingAndNullReturnEmpty() {
        assertThat(shipments.findById("nope")).isEmpty();
        assertThat(shipments.findById(null)).isEmpty();
        assertThat(shipments.findByOrderId(null)).isEmpty();
    }

    @Test
    void findByOrderIdReturnsOnlyThatOrdersShipmentsInCreationOrder() {
        Shipment cancelled = shipment("O1");
        cancelled.cancel();
        shipments.save(cancelled);
        Shipment active = shipments.save(shipment("O1"));
        shipments.save(shipment("O2"));

        assertThat(shipments.findByOrderId("O1")).extracting(Shipment::getId)
                .containsExactly(cancelled.getId(), active.getId());
        assertThat(shipments.findAll()).hasSize(3);
    }

    @Test
    void uniqueIndexBlocksSecondActiveShipmentOfSameOrder() {
        shipments.save(shipment("O1"));

        assertThatThrownBy(() -> shipments.save(shipment("O1"))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueIndexAllowsNewShipmentAfterCancelledOrReturned() {
        Shipment cancelled = shipment("O1");
        cancelled.cancel();
        shipments.save(cancelled);
        shipments.save(shipment("O1"));

        assertThat(shipments.findByOrderId("O1")).hasSize(2);
    }

    @Test
    void deleteRemovesShipmentAndEvents() {
        Shipment shipment = shipments.save(shipment("O1"));

        assertThat(shipments.deleteById(shipment.getId())).isTrue();
        assertThat(shipments.deleteById(shipment.getId())).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM shipment_events").query(Integer.class).single()).isZero();
    }
}
