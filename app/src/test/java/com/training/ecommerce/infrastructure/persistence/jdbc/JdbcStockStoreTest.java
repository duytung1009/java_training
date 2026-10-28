package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcStockStoreTest extends PostgresJdbcTestSupport {

    @Autowired
    private PlatformTransactionManager txManager;

    private JdbcProductRepository products;
    private JdbcStockStore store;

    @BeforeEach
    void setUpStore() {
        products = new JdbcProductRepository(jdbc);
        store = new JdbcStockStore(jdbc);
    }

    private Product product(String id, int stock) {
        return products.save(new PhysicalProduct(id, "SP " + id, new BigDecimal("100000"), stock, new BigDecimal("1")));
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

    private int stockOf(String id) {
        return jdbc.sql("SELECT stock FROM products WHERE id = :id").param("id", id).query(Integer.class).single();
    }

    @Test
    void decreasesStockForEveryLine() {
        Product a = product("A", 10);
        Product b = product("B", 5);

        store.decreaseStock(order(line(a, 3), line(b, 5)));

        assertThat(stockOf("A")).isEqualTo(7);
        assertThat(stockOf("B")).isZero();
    }

    @Test
    void sumsDuplicateLinesOfSameProduct() {
        Product a = product("A", 10);

        assertThatThrownBy(() -> store.decreaseStock(order(line(a, 6), line(a, 6)))).isInstanceOf(OutOfStockException.class);
        assertThat(stockOf("A")).isEqualTo(10);

        store.decreaseStock(order(line(a, 4), line(a, 6)));
        assertThat(stockOf("A")).isZero();
    }

    @Test
    void throwsOutOfStockWithDetails() {
        Product a = product("A", 2);
        Order order = order(line(a, 2));
        jdbc.sql("UPDATE products SET stock = 1 WHERE id = 'A'").update();   // hàng bị mua mất sau khi đơn được dựng

        assertThatThrownBy(() -> store.decreaseStock(order))
                .isInstanceOfSatisfying(OutOfStockException.class, e -> {
                    assertThat(e.getProductId()).isEqualTo("A");
                    assertThat(e.getRequested()).isEqualTo(2);
                    assertThat(e.getAvailable()).isEqualTo(1);
                });
        assertThat(stockOf("A")).isEqualTo(1);
    }

    @Test
    void throwsProductNotFoundForUnknownProduct() {
        Product ghost = new PhysicalProduct("GHOST", "Ma", BigDecimal.TEN, 5, BigDecimal.ONE);

        assertThatThrownBy(() -> store.decreaseStock(order(line(ghost, 1))))
                .isInstanceOfSatisfying(ProductNotFoundException.class, e -> assertThat(e.getProductId()).isEqualTo("GHOST"));
    }

    @Test
    void rejectsNullOrder() {
        assertThatThrownBy(() -> store.decreaseStock(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allOrNothingWhenCallerTransactionRollsBack() {
        Product a = product("A", 10);
        Product b = product("B", 1);
        Order order = order(line(a, 3), line(b, 1));
        jdbc.sql("UPDATE products SET stock = 0 WHERE id = 'B'").update();   // line thứ 2 sẽ thiếu hàng

        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> store.decreaseStock(order)))
                .isInstanceOf(OutOfStockException.class);

        assertThat(stockOf("A")).isEqualTo(10);     // line 1 đã được hoàn lại
        assertThat(stockOf("B")).isZero();
    }

    @Test
    void concurrentOrdersForLastUnitOnlyOneSucceeds() throws Exception {
        Product a = product("A", 1);
        int threads = 8;
        TransactionTemplate tx = new TransactionTemplate(txManager);

        List<Throwable> results = Concurrently.run(threads, i -> () ->
                tx.executeWithoutResult(status -> store.decreaseStock(order(line(a, 1)))));

        assertThat(results.stream().filter(r -> r == null)).hasSize(1);
        assertThat(results.stream().filter(r -> r != null))
                .hasSize(threads - 1)
                .allSatisfy(r -> assertThat(r).isInstanceOf(OutOfStockException.class));
        assertThat(stockOf("A")).isZero();
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        Product a = product("A", 20);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        List<Throwable> results = Concurrently.run(30, i -> () ->
                tx.executeWithoutResult(status -> store.decreaseStock(order(line(a, 1)))));

        assertThat(results.stream().filter(r -> r == null)).hasSize(20);
        assertThat(stockOf("A")).isZero();
    }
}
