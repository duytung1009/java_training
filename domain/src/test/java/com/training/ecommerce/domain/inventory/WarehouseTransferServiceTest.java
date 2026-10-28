// SPEC S06 – sinh bởi java-start-backlog 28/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("WarehouseTransferService (S06)")
class WarehouseTransferServiceTest {

    private static final String TSHIRT = "P001";

    private WarehouseTransferService service;
    private Warehouse hanoi;
    private Warehouse saigon;

    @BeforeEach
    void setUp() {
        service = new WarehouseTransferService();
        hanoi = new Warehouse("HN");
        saigon = new Warehouse("HCM");
    }

    @Nested
    @DisplayName("Đơn luồng")
    class SingleThread {

        @Test
        @DisplayName("HN có 100 áo, chuyển 30 sang HCM → HN 70, HCM 30")
        void movesStock() {
            hanoi.add(TSHIRT, 100);

            service.transfer(hanoi, saigon, TSHIRT, 30);

            assertEquals(70, hanoi.stockOf(TSHIRT));
            assertEquals(30, saigon.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Kho đích đã có hàng → cộng dồn")
        void addsToExistingStock() {
            hanoi.add(TSHIRT, 100);
            saigon.add(TSHIRT, 5);

            service.transfer(hanoi, saigon, TSHIRT, 30);

            assertEquals(35, saigon.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Thiếu hàng → OutOfStockException, cả hai kho giữ nguyên")
        void notEnoughStock() {
            hanoi.add(TSHIRT, 10);
            saigon.add(TSHIRT, 5);

            assertThrows(OutOfStockException.class, () -> service.transfer(hanoi, saigon, TSHIRT, 11));
            assertEquals(10, hanoi.stockOf(TSHIRT));
            assertEquals(5, saigon.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Kho nguồn chưa có product → OutOfStockException, kho đích không có hàng ảo")
        void unknownProductInSource() {
            assertThrows(OutOfStockException.class, () -> service.transfer(hanoi, saigon, TSHIRT, 1));
            assertEquals(0, saigon.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("from / to / productId null → IllegalArgumentException")
        void rejectsNulls() {
            hanoi.add(TSHIRT, 10);

            assertThrows(IllegalArgumentException.class, () -> service.transfer(null, saigon, TSHIRT, 1));
            assertThrows(IllegalArgumentException.class, () -> service.transfer(hanoi, null, TSHIRT, 1));
            assertThrows(IllegalArgumentException.class, () -> service.transfer(hanoi, saigon, null, 1));
            assertEquals(10, hanoi.stockOf(TSHIRT));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        @DisplayName("quantity <= 0 → IllegalArgumentException, cả hai kho giữ nguyên")
        void rejectsNonPositiveQuantity(int quantity) {
            hanoi.add(TSHIRT, 10);

            assertThrows(IllegalArgumentException.class, () -> service.transfer(hanoi, saigon, TSHIRT, quantity));
            assertEquals(10, hanoi.stockOf(TSHIRT));
            assertEquals(0, saigon.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Chuyển từ kho sang chính nó → IllegalArgumentException, tồn giữ nguyên")
        void rejectsSameWarehouse() {
            hanoi.add(TSHIRT, 10);

            assertThrows(IllegalArgumentException.class, () -> service.transfer(hanoi, hanoi, TSHIRT, 3));
            assertEquals(10, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Hai kho khác object nhưng trùng id → IllegalArgumentException")
        void rejectsSameId() {
            Warehouse hanoiCopy = new Warehouse("HN");
            hanoi.add(TSHIRT, 10);

            assertThrows(IllegalArgumentException.class, () -> service.transfer(hanoi, hanoiCopy, TSHIRT, 3));
            assertEquals(10, hanoi.stockOf(TSHIRT));
            assertEquals(0, hanoiCopy.stockOf(TSHIRT));
        }
    }

    @Nested
    @DisplayName("Nhiều thread cùng gọi")
    class Concurrent {

        @RepeatedTest(5)
        @DisplayName("HN→HCM và HCM→HN song song, mỗi chiều 10.000 lần → không treo, tổng tồn không đổi")
        void opposingTransfers_noDeadlock() throws Exception {
            hanoi.add(TSHIRT, 1000);
            saigon.add(TSHIRT, 1000);
            Runnable hnToHcm = () -> repeatTransfer(hanoi, saigon);
            Runnable hcmToHn = () -> repeatTransfer(saigon, hanoi);
            Thread t1 = new Thread(hnToHcm, "HN->HCM");
            Thread t2 = new Thread(hcmToHn, "HCM->HN");
            t1.setDaemon(true);                 // deadlock thì test fail thay vì treo cả JVM
            t2.setDaemon(true);

            t1.start();
            t2.start();
            t1.join(10_000);
            t2.join(10_000);

            assertFalse(t1.isAlive() || t2.isAlive(), "Có thread chưa xong sau 10 giây: nghi deadlock");
            assertEquals(2000, hanoi.stockOf(TSHIRT) + saigon.stockOf(TSHIRT));
        }

        @RepeatedTest(5)
        @DisplayName("transfer chạy song song với add/remove trực tiếp trên kho → tổng tồn đúng")
        void transferWithDirectAddRemove() throws Exception {
            hanoi.add(TSHIRT, 1000);
            saigon.add(TSHIRT, 1000);
            AtomicInteger removed = new AtomicInteger();

            List<Throwable> results = Concurrently.run(3, i -> switch (i) {
                case 0 -> () -> repeatTransfer(hanoi, saigon);
                case 1 -> () -> {
                    for (int k = 0; k < 5000; k++) {
                        hanoi.add(TSHIRT, 1);
                    }
                };
                default -> () -> {
                    for (int k = 0; k < 5000; k++) {
                        try {
                            saigon.remove(TSHIRT, 1);
                            removed.incrementAndGet();
                        } catch (OutOfStockException ignored) {
                            // hết hàng ở HCM tại thời điểm đó: bỏ qua
                        }
                    }
                };
            });

            results.forEach(r -> assertNull(r));
            assertEquals(2000 + 5000 - removed.get(), hanoi.stockOf(TSHIRT) + saigon.stockOf(TSHIRT));
        }

        /** Chuyển 1 áo mỗi lần, 10.000 lần; lúc kho nguồn tạm hết hàng thì bỏ qua lần đó. */
        private void repeatTransfer(Warehouse from, Warehouse to) {
            for (int k = 0; k < 10_000; k++) {
                try {
                    service.transfer(from, to, TSHIRT, 1);
                } catch (OutOfStockException ignored) {
                    // kho nguồn tạm hết hàng: bỏ qua lần này
                }
            }
        }
    }
}
