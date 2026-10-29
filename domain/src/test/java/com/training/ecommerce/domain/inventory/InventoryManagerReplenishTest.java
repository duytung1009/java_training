// SPEC S07-extra – sinh 30/10 (warehouse-hub). Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// Test concurrency xanh không chứng minh code không có race; vì vậy mỗi kịch bản được lặp nhiều lần (@RepeatedTest).
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InventoryManager.replenish: nhập hàng kho → hub (S07-extra)")
class InventoryManagerReplenishTest {

    private static final BigDecimal NO_RATE = BigDecimal.ZERO;
    private static final String LAPTOP = "LAPTOP";

    private InventoryManager hub;
    private Warehouse hanoi;
    private Warehouse saigon;
    private Product laptop;

    @BeforeEach
    void setUp() {
        hub = new InventoryManager();
        hanoi = new Warehouse("HN");
        saigon = new Warehouse("HCM");
        laptop = new PhysicalProduct(LAPTOP, "Laptop", new BigDecimal("20000000"), 5, new BigDecimal("2.0"));
        hub.addProduct(laptop);
    }

    @Nested
    @DisplayName("Đơn luồng")
    class SingleThread {

        @Test
        @DisplayName("HN có 10 laptop, hub 5, nhập 4 → HN 6, hub 9")
        void movesStockFromWarehouseToHub() {
            hanoi.add(LAPTOP, 10);

            hub.replenish(hanoi, LAPTOP, 4);

            assertEquals(6, hanoi.stockOf(LAPTOP));
            assertEquals(9, laptop.getStock());
        }

        @Test
        @DisplayName("Kho có 3, nhập 4 → OutOfStockException, hai bên giữ nguyên")
        void notEnoughInWarehouse() {
            hanoi.add(LAPTOP, 3);

            OutOfStockException e = assertThrows(OutOfStockException.class, () -> hub.replenish(hanoi, LAPTOP, 4));
            assertEquals(4, e.getRequested());
            assertEquals(3, e.getAvailable());
            assertEquals(3, hanoi.stockOf(LAPTOP));
            assertEquals(5, laptop.getStock());
        }

        @Test
        @DisplayName("Kho chưa có product → OutOfStockException, hub giữ nguyên")
        void unknownProductInWarehouse() {
            assertThrows(OutOfStockException.class, () -> hub.replenish(hanoi, LAPTOP, 1));
            assertEquals(5, laptop.getStock());
        }

        @Test
        @DisplayName("Product không có trên hub → ProductNotFoundException, kho giữ nguyên (D4)")
        void productNotInHub() {
            hanoi.add("PHONE", 6);

            assertThrows(ProductNotFoundException.class, () -> hub.replenish(hanoi, "PHONE", 2));
            assertEquals(6, hanoi.stockOf("PHONE"));
            assertEquals(List.of(), hub.search(p -> p.getId().equals("PHONE")), "không tự tạo product từ kho");
        }

        @Test
        @DisplayName("from / productId null → IllegalArgumentException, hai bên giữ nguyên")
        void rejectsNulls() {
            hanoi.add(LAPTOP, 10);

            assertThrows(IllegalArgumentException.class, () -> hub.replenish(null, LAPTOP, 1));
            assertThrows(IllegalArgumentException.class, () -> hub.replenish(hanoi, null, 1));
            assertEquals(10, hanoi.stockOf(LAPTOP));
            assertEquals(5, laptop.getStock());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        @DisplayName("quantity <= 0 → IllegalArgumentException, hai bên giữ nguyên")
        void rejectsNonPositiveQuantity(int quantity) {
            hanoi.add(LAPTOP, 10);

            assertThrows(IllegalArgumentException.class, () -> hub.replenish(hanoi, LAPTOP, quantity));
            assertEquals(10, hanoi.stockOf(LAPTOP));
            assertEquals(5, laptop.getStock());
        }
    }

    @Nested
    @DisplayName("Nhiều thread cùng gọi")
    class Concurrent {

        @RepeatedTest(5)
        @DisplayName("replenish + transfer hai chiều + bán chạy song song → không treo, tổng hub + kho + đã bán không đổi")
        void replenishTransferSell_conserveTotal() throws Exception {
            hanoi.add(LAPTOP, 3000);
            saigon.add(LAPTOP, 1000);
            int total = laptop.getStock() + 3000 + 1000;
            Order oneLaptop = orderOf(new OrderLine(laptop, 1));     // Order không bị sửa khi trừ kho nên dùng chung được
            WarehouseTransferService transfers = new WarehouseTransferService();
            AtomicInteger sold = new AtomicInteger();
            List<Throwable> crashes = new CopyOnWriteArrayList<>();

            List<Thread> threads = List.of(
                    daemon("replenish HN", () -> repeat(() -> hub.replenish(hanoi, LAPTOP, 1))),
                    daemon("HN->HCM", () -> repeat(() -> transfers.transfer(hanoi, saigon, LAPTOP, 1))),
                    daemon("HCM->HN", () -> repeat(() -> transfers.transfer(saigon, hanoi, LAPTOP, 1))),
                    daemon("sell", () -> repeat(() -> {
                        hub.decreaseStock(oneLaptop);
                        sold.incrementAndGet();
                    })));
            threads.forEach(t -> t.setUncaughtExceptionHandler((thread, e) -> crashes.add(e)));
            threads.forEach(Thread::start);
            for (Thread t : threads) {
                t.join(10_000);
            }

            assertFalse(threads.stream().anyMatch(Thread::isAlive), "Có thread chưa xong sau 10 giây: nghi deadlock");
            assertEquals(List.of(), crashes, "chỉ được ném OutOfStockException");
            assertEquals(total, laptop.getStock() + hanoi.stockOf(LAPTOP) + saigon.stockOf(LAPTOP) + sold.get());
        }

        @RepeatedTest(20)
        @DisplayName("Hub hết hàng, 1 thread nhập 1 cái, 100 thread cùng mua → tối đa 1 người mua được, không âm")
        void replenishWhileBuyingLastItem() throws Exception {
            Product phone = new PhysicalProduct("PHONE", "Điện thoại", new BigDecimal("12000000"), 1,
                    new BigDecimal("0.4"));
            hub.addProduct(phone);
            Order onePhone = orderOf(new OrderLine(phone, 1));     // OrderLine cần stock > 0 lúc tạo
            hub.decreaseStock(onePhone);                            // hub còn 0
            hanoi.add("PHONE", 1);

            List<Throwable> results = Concurrently.run(101, i -> i == 0
                    ? () -> hub.replenish(hanoi, "PHONE", 1)
                    : () -> hub.decreaseStock(onePhone));

            assertNull(results.getFirst(), "replenish phải thành công");
            long bought = results.subList(1, results.size()).stream().filter(r -> r == null).count();
            results.subList(1, results.size()).stream().filter(r -> r != null)
                    .forEach(r -> assertInstanceOf(OutOfStockException.class, r));
            assertEquals(0, hanoi.stockOf("PHONE"));
            assertEquals(1, bought + phone.getStock(), "cái nhập lên hoặc đã bán, hoặc còn trên hub");
        }
    }

    /** Chạy action 5000 lần, bỏ qua OutOfStockException (hết hàng tạm thời là bình thường khi chạy song song). */
    private static void repeat(Runnable action) {
        for (int i = 0; i < 5000; i++) {
            try {
                action.run();
            } catch (OutOfStockException ignored) {
                // nguồn tạm hết hàng, thử lần sau
            }
        }
    }

    private static Thread daemon(String name, Runnable task) {
        Thread t = new Thread(task, name);
        t.setDaemon(true);                 // deadlock thì test fail thay vì treo cả JVM
        return t;
    }

    private static Order orderOf(OrderLine... lines) {
        Order order = new Order(NO_RATE, NO_RATE);
        for (OrderLine line : lines) {
            order.addLine(line);
        }
        return order;
    }
}
