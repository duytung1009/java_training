package com.training.ecommerce.demo;

import com.training.ecommerce.application.order.OrderDetail;
import com.training.ecommerce.application.order.OrderDetailService;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.inventory.Warehouse;
import com.training.ecommerce.domain.inventory.WarehouseTransferService;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.infrastructure.fake.FakeOrderLineSource;
import com.training.ecommerce.infrastructure.fake.FakePaymentGateway;
import com.training.ecommerce.infrastructure.fake.FakeShipmentTracker;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo S06: flash sale 100 thread, chuyển kho hai chiều không deadlock, tải chi tiết đơn tuần tự / fixed pool /
 * virtual thread. Cần JDK 21+ (virtual thread), try-with-resources cho ExecutorService từ Java 19.
 */
public class S06Demo {

    private static final Duration SOURCE_DELAY = Duration.ofMillis(300);

    public static void main(String[] args) throws Exception {
        flashSale();
        warehouseTransfer();
        orderDetail();
    }

    // ---------- 1. Flash sale: 100 khách tranh chiếc áo cuối cùng ----------

    private static void flashSale() throws Exception {
        InventoryManager inventory = new InventoryManager();
        Product tshirt = new PhysicalProduct("P001", "Áo thun nam (flash sale)", new BigDecimal("99000"), 1,
                new BigDecimal("0.3"), Category.FASHION);
        inventory.addProduct(tshirt);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Order order = new Order(BigDecimal.ZERO, new BigDecimal("0.08"));
            order.addLine(new OrderLine(tshirt, 1));
            orders.add(order);
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);            // cho 100 thread xuất phát cùng lúc
        try (ExecutorService pool = Executors.newFixedThreadPool(100)) {
            for (Order order : orders) {
                pool.submit(() -> {
                    gate.await();
                    try {
                        inventory.decreaseStock(order);
                        success.incrementAndGet();
                    } catch (OutOfStockException e) {
                        outOfStock.incrementAndGet();
                    }
                    return null;
                });
            }
            gate.countDown();
        }                                                       // close(): chờ cả 100 task xong
        System.out.println(">>> Flash sale: " + success + " đơn thành công, " + outOfStock
                + " đơn hết hàng, tồn còn " + tshirt.getStock());
    }

    // ---------- 2. Chuyển kho hai chiều cùng lúc ----------

    private static void warehouseTransfer() throws InterruptedException {
        Warehouse hanoi = new Warehouse("HN");
        Warehouse saigon = new Warehouse("HCM");
        hanoi.add("P002", 500);
        saigon.add("P002", 500);
        WarehouseTransferService service = new WarehouseTransferService();

        Thread north = new Thread(() -> repeatTransfer(service, hanoi, saigon), "HN->HCM");
        Thread south = new Thread(() -> repeatTransfer(service, saigon, hanoi), "HCM->HN");
        long start = System.nanoTime();
        north.start();
        south.start();
        north.join();
        south.join();
        System.out.printf(">>> Chuyển kho 2 chiều × 10.000 lần: xong sau %d ms, HN %d + HCM %d = %d (không đổi)%n",
                elapsedMs(start), hanoi.stockOf("P002"), saigon.stockOf("P002"),
                hanoi.stockOf("P002") + saigon.stockOf("P002"));

        try {
            service.transfer(hanoi, saigon, "P002", 10_000);
        } catch (OutOfStockException e) {
            System.out.println(">>> Chuyển quá tồn: " + e.getMessage() + " → hai kho giữ nguyên");
        }
    }

    private static void repeatTransfer(WarehouseTransferService service, Warehouse from, Warehouse to) {
        for (int i = 0; i < 10_000; i++) {
            try {
                service.transfer(from, to, "P002", 1);
            } catch (OutOfStockException ignored) {
                // kho nguồn tạm hết hàng: bỏ qua lần này
            }
        }
    }

    // ---------- 3. Trang chi tiết đơn: 3 nguồn, mỗi nguồn 300 ms ----------

    private static void orderDetail() throws Exception {
        Product jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20,
                new BigDecimal("0.8"), Category.FASHION);
        FakePaymentGateway payments = new FakePaymentGateway(SOURCE_DELAY, new BigDecimal("972000.00"));
        FakeShipmentTracker shipments = new FakeShipmentTracker(SOURCE_DELAY);
        FakeOrderLineSource lines = new FakeOrderLineSource(SOURCE_DELAY, List.of(new OrderLine(jeans, 2)));

        // Tuần tự: gọi lần lượt trên thread hiện tại
        long start = System.nanoTime();
        new OrderDetail("DH001", payments.findPayment("DH001"), shipments.track("DH001"), lines.findLines("DH001"));
        System.out.printf(">>> 1 đơn, gọi tuần tự:           %4d ms%n", elapsedMs(start));

        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            OrderDetailService service = new OrderDetailService(payments, shipments, lines, pool);
            start = System.nanoTime();
            OrderDetail detail = service.loadDetail("DH001");
            System.out.printf(">>> 1 đơn, fixedThreadPool(3):    %4d ms → %s, %s, %d dòng%n", elapsedMs(start),
                    detail.payment().status(), detail.shipment().status(), detail.lines().size());
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            OrderDetailService service = new OrderDetailService(payments, shipments, lines, virtual);
            start = System.nanoTime();
            service.loadDetail("DH001");
            System.out.printf(">>> 1 đơn, virtual thread:        %4d ms%n", elapsedMs(start));
        }

        // 200 khách mở trang chi tiết cùng lúc = 600 lời gọi nguồn
        System.out.printf(">>> 200 đơn, fixedThreadPool(30): %4d ms (30 lời gọi một lúc)%n",
                loadMany(Executors.newFixedThreadPool(30), payments, shipments, lines));
        System.out.printf(">>> 200 đơn, virtual thread:      %4d ms (600 lời gọi cùng chờ IO)%n",
                loadMany(Executors.newVirtualThreadPerTaskExecutor(), payments, shipments, lines));
    }

    /** 200 caller (virtual thread) cùng gọi loadDetail trên executor cho trước; trả về thời gian (ms). */
    private static long loadMany(ExecutorService sourceExecutor, FakePaymentGateway payments,
                                 FakeShipmentTracker shipments, FakeOrderLineSource lines) throws Exception {
        try (sourceExecutor; ExecutorService callers = Executors.newVirtualThreadPerTaskExecutor()) {
            OrderDetailService service = new OrderDetailService(payments, shipments, lines, sourceExecutor);
            long start = System.nanoTime();
            List<Future<OrderDetail>> results = new ArrayList<>();
            for (int i = 1; i <= 200; i++) {
                String orderId = "DH%03d".formatted(i);
                results.add(callers.submit(() -> service.loadDetail(orderId)));
            }
            for (Future<OrderDetail> result : results) {
                result.get();
            }
            return elapsedMs(start);
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
