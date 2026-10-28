// SPEC S06 – sinh bởi java-start-backlog 28/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// Test concurrency xanh không chứng minh code không có race; vì vậy mỗi kịch bản được lặp nhiều lần (@RepeatedTest).
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InventoryManager khi nhiều thread cùng gọi (S06)")
class InventoryManagerConcurrencyTest {

    private static final BigDecimal NO_RATE = BigDecimal.ZERO;

    private InventoryManager inventory;

    @BeforeEach
    void setUp() {
        inventory = new InventoryManager();
    }

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStock {

        @RepeatedTest(20)
        @DisplayName("100 khách cùng mua chiếc áo cuối cùng → đúng 1 người mua được, stock = 0")
        void lastItem_exactlyOneBuyerWins() throws Exception {
            Product tshirt = physical("P001", "Áo thun nam", "150000", 1);
            inventory.addProduct(tshirt);
            List<Order> orders = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                orders.add(orderOf(new OrderLine(tshirt, 1)));      // tạo đơn trước khi stock bị trừ
            }

            List<Throwable> results = Concurrently.run(100, i -> () -> inventory.decreaseStock(orders.get(i)));

            long successes = results.stream().filter(r -> r == null).count();
            assertEquals(1, successes, "đúng 1 đơn thành công");
            results.stream().filter(r -> r != null)
                    .forEach(r -> assertInstanceOf(OutOfStockException.class, r, "đơn thất bại phải ném OutOfStockException"));
            assertEquals(0, tshirt.getStock());
        }

        @RepeatedTest(10)
        @DisplayName("Kho 10.000 áo, 100 thread × 100 đơn (1 áo/đơn) → mọi đơn thành công, stock đúng 0 (không mất lần trừ nào)")
        void manyOrders_noLostUpdate() throws Exception {
            Product tshirt = physical("P001", "Áo thun nam", "150000", 10_000);
            inventory.addProduct(tshirt);
            Order oneShirt = orderOf(new OrderLine(tshirt, 1));        // Order không bị sửa khi trừ kho nên dùng chung được

            List<Throwable> results = Concurrently.run(100, i -> () -> {
                for (int k = 0; k < 100; k++) {
                    inventory.decreaseStock(oneShirt);
                }
            });

            results.forEach(r -> assertNull(r, () -> "không đơn nào được lỗi: " + r));
            assertEquals(0, tshirt.getStock());
        }

        @RepeatedTest(20)
        @DisplayName("Đơn nhiều dòng chạy song song vẫn all-or-nothing: tồn kho khớp đúng với số đơn thành công")
        void multiLineOrders_allOrNothingUnderConcurrency() throws Exception {
            Product tshirt = physical("P001", "Áo thun nam", "150000", 30);
            Product jeans = physical("P002", "Quần jean", "450000", 30);
            inventory.addProduct(tshirt);
            inventory.addProduct(jeans);
            List<Order> orders = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                orders.add(i % 2 == 0
                        ? orderOf(new OrderLine(tshirt, 2), new OrderLine(jeans, 1))   // đơn chẵn: 2 áo + 1 quần
                        : orderOf(new OrderLine(jeans, 2)));                           // đơn lẻ: 2 quần
            }

            List<Throwable> results = Concurrently.run(60, i -> () -> inventory.decreaseStock(orders.get(i)));

            int evenSuccesses = 0;
            int oddSuccesses = 0;
            for (int i = 0; i < results.size(); i++) {
                Throwable r = results.get(i);
                if (r == null) {
                    if (i % 2 == 0) evenSuccesses++; else oddSuccesses++;
                } else {
                    assertInstanceOf(OutOfStockException.class, r);
                }
            }
            assertTrue(evenSuccesses + oddSuccesses > 0, "phải có đơn thành công");
            assertEquals(30 - 2 * evenSuccesses, tshirt.getStock(), "áo chỉ bị trừ bởi đơn chẵn thành công");
            assertEquals(30 - evenSuccesses - 2 * oddSuccesses, jeans.getStock(), "quần bị trừ đúng theo đơn thành công");
            assertTrue(tshirt.getStock() >= 0 && jeans.getStock() >= 0, "tồn kho không bao giờ âm");
        }
    }

    @Nested
    @DisplayName("addProduct / search")
    class AddAndSearch {

        @RepeatedTest(10)
        @DisplayName("100 thread thêm 100 product khác id → đủ cả 100, tìm được từng cái")
        void addDistinctIds_noneLost() throws Exception {
            List<Throwable> results = Concurrently.run(100,
                    i -> () -> inventory.addProduct(physical("P" + (100 + i), "Sản phẩm " + i, "99000", 10)));

            results.forEach(r -> assertNull(r, "không thread nào được lỗi"));
            assertEquals(100, inventory.search(p -> true).size());
            for (int i = 0; i < 100; i++) {
                assertTrue(inventory.findById("P" + (100 + i)).isPresent(), "thiếu P" + (100 + i));
            }
        }

        @RepeatedTest(10)
        @DisplayName("50 thread cùng thêm một id → đúng 1 lần thành công, 49 lần IllegalArgumentException")
        void addSameId_exactlyOneWins() throws Exception {
            List<Throwable> results = Concurrently.run(50,
                    i -> () -> inventory.addProduct(physical("P500", "Tai nghe " + i, "350000", 5)));

            assertEquals(1, results.stream().filter(r -> r == null).count());
            results.stream().filter(r -> r != null).forEach(r -> assertInstanceOf(IllegalArgumentException.class, r));
            assertEquals(1, inventory.search(p -> true).size());
        }

        @Test
        @DisplayName("search trong lúc thread khác đang addProduct → không ném exception")
        void searchWhileAdding_noException() throws Exception {
            int toAdd = 3000;
            CountDownLatch writerDone = new CountDownLatch(1);
            List<Throwable> results = Concurrently.run(5, i -> i == 0
                    ? () -> {
                        try {
                            for (int k = 0; k < toAdd; k++) {
                                inventory.addProduct(physical("P" + (10_000 + k), "Ốp lưng " + k, "50000", 1));
                            }
                        } finally {
                            writerDone.countDown();
                        }
                    }
                    : () -> {
                        while (writerDone.getCount() > 0) {
                            inventory.search(p -> p.getStock() > 0);
                        }
                    });

            results.forEach(r -> assertNull(r, () -> "không được ném " + r));
            assertEquals(toAdd, inventory.search(p -> true).size());
        }
    }

    // ---------- helpers ----------

    private static Product physical(String id, String name, String price, int stock) {
        return new PhysicalProduct(id, name, new BigDecimal(price), stock, new BigDecimal("0.5"));
    }

    private static Order orderOf(OrderLine... lines) {
        Order order = new Order(NO_RATE, NO_RATE);
        for (OrderLine line : lines) {
            order.addLine(line);
        }
        return order;
    }
}
