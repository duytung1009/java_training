package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcOrderRepositoryTest extends PostgresJdbcTestSupport {

    private JdbcProductRepository products;
    private JdbcOrderRepository orders;

    @BeforeEach
    void setUpRepos() {
        products = new JdbcProductRepository(jdbc);
        orders = new JdbcOrderRepository(jdbc, products);
        products.save(new PhysicalProduct("P1", "Áo", new BigDecimal("150000"), 50, new BigDecimal("0.5")));
        products.save(new PhysicalProduct("P2", "Quần", new BigDecimal("200000"), 50, new BigDecimal("0.7")));
    }

    private Order newOrder(String id) {
        Order order = new Order(id, new BigDecimal("0.1"), new BigDecimal("0.08"));
        order.addLine(new OrderLine(products.findById("P1").orElseThrow(), 2));
        order.addLine(new OrderLine(products.findById("P2").orElseThrow(), 1));
        return order;
    }

    @Test
    void roundTripKeepsRatesLinesAndTotals() {
        Order saved = orders.save(newOrder("O1"));

        Order found = orders.findById("O1").orElseThrow();
        assertThat(found.getDiscountRate()).isEqualByComparingTo("0.1");
        assertThat(found.getTaxRate()).isEqualByComparingTo("0.08");
        assertThat(found.getLines()).hasSize(2);
        assertThat(found.getLines()[0].getProduct().getId()).isEqualTo("P1");
        assertThat(found.getLines()[0].getQuantity()).isEqualTo(2);
        assertThat(found.getLines()[1].getProduct().getId()).isEqualTo("P2");
        assertThat(found.finalAmount()).isEqualByComparingTo(saved.finalAmount());
    }

    @Test
    void unitPriceSnapshotSurvivesProductPriceChange() {
        orders.save(newOrder("O1"));
        products.save(new PhysicalProduct("P1", "Áo", new BigDecimal("999999"), 50, new BigDecimal("0.5")));

        OrderLine line = orders.findById("O1").orElseThrow().getLines()[0];

        assertThat(line.getProduct().getBasePrice()).isEqualByComparingTo("999999");
        assertThat(line.getUnitPrice()).isEqualByComparingTo("150000");
        assertThat(line.lineTotal()).isEqualByComparingTo("300000");
    }

    @Test
    void saveTwiceReplacesLinesAndKeepsPosition() {
        orders.save(newOrder("O1"));
        orders.save(newOrder("O2"));

        Order replaced = new Order("O1", BigDecimal.ZERO, BigDecimal.ZERO);
        replaced.addLine(new OrderLine(products.findById("P2").orElseThrow(), 3));
        orders.save(replaced);

        assertThat(orders.count()).isEqualTo(2);
        assertThat(orders.findAll()).extracting(Order::getId).containsExactly("O1", "O2");
        Order found = orders.findById("O1").orElseThrow();
        assertThat(found.getLines()).hasSize(1);
        assertThat(found.getLines()[0].getQuantity()).isEqualTo(3);
        assertThat(found.getDiscountRate()).isEqualByComparingTo("0");
    }

    @Test
    void findAllIsUnmodifiableAndFindByIdHandlesNullAndMissing() {
        orders.save(newOrder("O1"));

        List<Order> all = orders.findAll();

        assertThatThrownBy(() -> all.add(newOrder("X"))).isInstanceOf(UnsupportedOperationException.class);
        assertThat(orders.findById(null)).isEmpty();
        assertThat(orders.findById("nope")).isEmpty();
    }

    @Test
    void deleteRemovesOrderAndLines() {
        orders.save(newOrder("O1"));

        assertThat(orders.deleteById("O1")).isTrue();
        assertThat(orders.deleteById("O1")).isFalse();
        assertThat(orders.count()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM order_lines").query(Integer.class).single()).isZero();
    }

    @Test
    void saveRejectsNull() {
        assertThatThrownBy(() -> orders.save(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productCountUnaffected() {
        orders.save(newOrder("O1"));
        Product p = products.findById("P1").orElseThrow();
        assertThat(p.getStock()).isEqualTo(50);   // lưu đơn không tự trừ kho (việc của StockStore)
    }
}
