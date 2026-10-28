// SPEC S06 – sinh bởi java-start-backlog 28/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// Test "song song" dùng CountDownLatch thay vì đo thời gian, để không flaky.
package com.training.ecommerce.application.order;

import com.training.ecommerce.application.payment.PaymentGateway;
import com.training.ecommerce.application.payment.PaymentInfo;
import com.training.ecommerce.application.shipping.ShipmentInfo;
import com.training.ecommerce.application.shipping.ShipmentTracker;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OrderDetailService (S06)")
class OrderDetailServiceTest {

    private static final String ORDER_ID = "DH001";

    private final PaymentInfo paid = new PaymentInfo("PAID", new BigDecimal("300000.00"));
    private final ShipmentInfo delivering = new ShipmentInfo("GHN", "DELIVERING");
    private Product tshirt;
    private List<OrderLine> lines;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.5"));
        lines = new ArrayList<>(List.of(new OrderLine(tshirt, 2)));
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();     // test tạo executor thì test đóng
    }

    @Nested
    @DisplayName("Records")
    class Records {

        @Test
        @DisplayName("PaymentInfo / ShipmentInfo giữ đúng giá trị")
        void keepValues() {
            assertEquals("PAID", paid.status());
            assertEquals(new BigDecimal("300000.00"), paid.amount());
            assertEquals("GHN", delivering.carrier());
            assertEquals("DELIVERING", delivering.status());
        }

        @Test
        @DisplayName("Field null → IllegalArgumentException")
        void rejectNulls() {
            assertThrows(IllegalArgumentException.class, () -> new PaymentInfo(null, BigDecimal.TEN));
            assertThrows(IllegalArgumentException.class, () -> new PaymentInfo("PAID", null));
            assertThrows(IllegalArgumentException.class, () -> new ShipmentInfo(null, "DELIVERING"));
            assertThrows(IllegalArgumentException.class, () -> new ShipmentInfo("GHN", null));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetail(null, paid, delivering, lines));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetail(ORDER_ID, null, delivering, lines));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetail(ORDER_ID, paid, null, lines));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetail(ORDER_ID, paid, delivering, null));
        }

        @Test
        @DisplayName("OrderDetail.lines là bản copy không sửa được")
        void orderDetailCopiesLines() {
            OrderDetail detail = new OrderDetail(ORDER_ID, paid, delivering, lines);
            lines.add(new OrderLine(tshirt, 1));

            assertEquals(1, detail.lines().size(), "sửa list gốc không ảnh hưởng OrderDetail");
            assertThrows(UnsupportedOperationException.class, () -> detail.lines().add(new OrderLine(tshirt, 1)));
        }
    }

    @Nested
    @DisplayName("loadDetail – happy path")
    class HappyPath {

        @Test
        @DisplayName("Gộp đủ 3 nguồn; mỗi nguồn nhận đúng orderId")
        void combinesThreeSources() {
            List<String> receivedIds = new CopyOnWriteArrayList<>();
            OrderDetailService service = new OrderDetailService(
                    id -> { receivedIds.add(id); return paid; },
                    id -> { receivedIds.add(id); return delivering; },
                    id -> { receivedIds.add(id); return lines; },
                    executor);

            OrderDetail detail = service.loadDetail(ORDER_ID);

            assertEquals(ORDER_ID, detail.orderId());
            assertSame(paid, detail.payment());
            assertSame(delivering, detail.shipment());
            assertEquals(lines, detail.lines());
            assertEquals(List.of(ORDER_ID, ORDER_ID, ORDER_ID), receivedIds);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"fixedThreadPool(3)", "virtualThreadPerTask"})
        @DisplayName("3 nguồn chạy song song thật: mỗi nguồn chờ 2 nguồn kia bắt đầu (gọi tuần tự sẽ lỗi)")
        void callsSourcesInParallel(String kind) {
            ExecutorService pool = kind.startsWith("fixed")
                    ? Executors.newFixedThreadPool(3)
                    : Executors.newVirtualThreadPerTaskExecutor();
            try {
                CountDownLatch allStarted = new CountDownLatch(3);
                OrderDetailService service = new OrderDetailService(
                        id -> waitForOthers(allStarted, paid),
                        id -> waitForOthers(allStarted, delivering),
                        id -> waitForOthers(allStarted, lines),
                        pool);

                OrderDetail detail = service.loadDetail(ORDER_ID);

                assertSame(paid, detail.payment());
                assertSame(delivering, detail.shipment());
                assertEquals(lines, detail.lines());
            } finally {
                pool.shutdownNow();
            }
        }

        @Test
        @DisplayName("Service không shutdown executor được inject; gọi lại lần 2 vẫn chạy")
        void doesNotShutDownInjectedExecutor() {
            OrderDetailService service = new OrderDetailService(id -> paid, id -> delivering, id -> lines, executor);

            service.loadDetail(ORDER_ID);
            assertFalse(executor.isShutdown(), "bên nào tạo executor thì bên đó đóng");

            assertEquals(ORDER_ID, service.loadDetail("DH001").orderId());
        }
    }

    @Nested
    @DisplayName("Tham số không hợp lệ")
    class Validation {

        @Test
        @DisplayName("Constructor nhận null → IllegalArgumentException")
        void constructorRejectsNulls() {
            PaymentGateway p = id -> paid;
            ShipmentTracker s = id -> delivering;
            OrderLineSource l = id -> lines;

            assertThrows(IllegalArgumentException.class, () -> new OrderDetailService(null, s, l, executor));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetailService(p, null, l, executor));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetailService(p, s, null, executor));
            assertThrows(IllegalArgumentException.class, () -> new OrderDetailService(p, s, l, null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("orderId null / rỗng / khoảng trắng → IllegalArgumentException, không gọi nguồn nào")
        void rejectsBlankOrderId(String orderId) {
            AtomicInteger calls = new AtomicInteger();
            OrderDetailService service = new OrderDetailService(
                    id -> { calls.incrementAndGet(); return paid; },
                    id -> { calls.incrementAndGet(); return delivering; },
                    id -> { calls.incrementAndGet(); return lines; },
                    executor);

            assertThrows(IllegalArgumentException.class, () -> service.loadDetail(orderId));
            assertEquals(0, calls.get());
        }
    }

    @Nested
    @DisplayName("Lỗi và interrupt")
    class Failures {

        @ParameterizedTest(name = "nguồn lỗi: {0}")
        @ValueSource(strings = {"payment", "shipment", "lines"})
        @DisplayName("Một nguồn ném exception → OrderDetailLoadException, cause là đúng exception gốc")
        void sourceFailureIsWrapped(String failing) {
            IllegalStateException boom = new IllegalStateException("Mất kết nối tới " + failing);
            OrderDetailService service = new OrderDetailService(
                    id -> { if (failing.equals("payment")) throw boom; return paid; },
                    id -> { if (failing.equals("shipment")) throw boom; return delivering; },
                    id -> { if (failing.equals("lines")) throw boom; return lines; },
                    executor);

            OrderDetailLoadException e = assertThrows(OrderDetailLoadException.class, () -> service.loadDetail(ORDER_ID));
            assertSame(boom, e.getCause());
        }

        @Test
        @DisplayName("Thread gọi loadDetail bị interrupt khi đang chờ → OrderDetailLoadException và cờ interrupt vẫn bật")
        void interruptIsNotSwallowed() throws Exception {
            CountDownLatch started = new CountDownLatch(3);
            CountDownLatch release = new CountDownLatch(1);
            OrderDetailService service = new OrderDetailService(
                    id -> block(started, release, paid),
                    id -> block(started, release, delivering),
                    id -> block(started, release, lines),
                    executor);
            AtomicReference<Throwable> thrown = new AtomicReference<>();
            AtomicBoolean flagAfter = new AtomicBoolean();

            Thread caller = new Thread(() -> {
                try {
                    service.loadDetail(ORDER_ID);
                } catch (RuntimeException e) {
                    thrown.set(e);
                }
                flagAfter.set(Thread.currentThread().isInterrupted());
            }, "caller");
            caller.start();
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS), "3 nguồn chưa được gọi");
                caller.interrupt();
                caller.join(5_000);

                assertFalse(caller.isAlive(), "loadDetail không phản ứng với interrupt");
                assertInstanceOf(OrderDetailLoadException.class, thrown.get());
                assertTrue(flagAfter.get(), "cờ interrupt phải được bật lại, không bị nuốt");
            } finally {
                release.countDown();
            }
        }

        @Test
        @Tag("bonus")
        @DisplayName("BONUS: payment lỗi ngay → ném nhanh và hủy (cancel) các task còn lại đang chạy chậm")
        void failureCancelsRemainingTasks() throws Exception {
            AtomicInteger running = new AtomicInteger();
            AtomicInteger finishedNormally = new AtomicInteger();
            OrderDetailService service = new OrderDetailService(
                    id -> { throw new IllegalStateException("Cổng thanh toán mất kết nối"); },
                    id -> slow(running, finishedNormally, delivering),
                    id -> slow(running, finishedNormally, lines),
                    executor);

            long start = System.nanoTime();
            assertThrows(OrderDetailLoadException.class, () -> service.loadDetail(ORDER_ID));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertTrue(elapsedMs < 2_000, "phải ném ngay, không chờ nguồn chậm (mất " + elapsedMs + " ms)");
            Thread.sleep(300);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (running.get() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(0, running.get(), "task còn lại phải bị cancel(true) → interrupt");
            assertEquals(0, finishedNormally.get());
        }
    }

    // ---------- fake sources ----------

    /** Báo đã bắt đầu rồi chờ 2 nguồn kia; nếu bị gọi tuần tự thì sau 2 giây ném lỗi. */
    private static <T> T waitForOthers(CountDownLatch allStarted, T value) {
        allStarted.countDown();
        try {
            if (!allStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("3 nguồn không chạy song song");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return value;
    }

    /** Giả lập nguồn chậm: chờ tới khi test cho phép (tối đa 10 giây). */
    private static <T> T block(CountDownLatch started, CountDownLatch release, T value) {
        started.countDown();
        try {
            release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return value;
    }

    /** Giả lập nguồn chậm 5 giây; bị interrupt thì dừng sớm. */
    private static <T> T slow(AtomicInteger running, AtomicInteger finishedNormally, T value) {
        running.incrementAndGet();
        try {
            Thread.sleep(5_000);
            finishedNormally.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running.decrementAndGet();
        }
        return value;
    }
}
