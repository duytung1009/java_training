// SPEC S06 – sinh bởi java-start-backlog 28/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Warehouse (S06)")
class WarehouseTest {

    private static final String TSHIRT = "P001";
    private static final String JEANS = "P002";

    private Warehouse hanoi;

    @BeforeEach
    void setUp() {
        hanoi = new Warehouse("HN");
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("Giữ đúng id")
        void keepsId() {
            assertEquals("HN", hanoi.getId());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("id null / rỗng / chỉ khoảng trắng → IllegalArgumentException")
        void rejectsBlankId(String id) {
            assertThrows(IllegalArgumentException.class, () -> new Warehouse(id));
        }
    }

    @Nested
    @DisplayName("stockOf / add")
    class StockAndAdd {

        @Test
        @DisplayName("Product chưa có trong kho → tồn 0")
        void unknownProductHasZeroStock() {
            assertEquals(0, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("add cộng dồn, mỗi product một số riêng")
        void addAccumulatesPerProduct() {
            hanoi.add(TSHIRT, 30);
            hanoi.add(TSHIRT, 20);
            hanoi.add(JEANS, 5);

            assertEquals(50, hanoi.stockOf(TSHIRT));
            assertEquals(5, hanoi.stockOf(JEANS));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("add với quantity <= 0 → IllegalArgumentException, tồn giữ nguyên")
        void addRejectsNonPositiveQuantity(int quantity) {
            hanoi.add(TSHIRT, 10);

            assertThrows(IllegalArgumentException.class, () -> hanoi.add(TSHIRT, quantity));
            assertEquals(10, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("productId null → IllegalArgumentException (stockOf, add)")
        void rejectsNullProductId() {
            assertThrows(IllegalArgumentException.class, () -> hanoi.stockOf(null));
            assertThrows(IllegalArgumentException.class, () -> hanoi.add(null, 10));
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @BeforeEach
        void stockUp() {
            hanoi.add(TSHIRT, 50);
        }

        @Test
        @DisplayName("Xuất 20 trong 50 → còn 30; xuất hết → còn 0")
        void removesQuantity() {
            hanoi.remove(TSHIRT, 20);
            assertEquals(30, hanoi.stockOf(TSHIRT));

            hanoi.remove(TSHIRT, 30);
            assertEquals(0, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Xuất quá tồn → OutOfStockException có đủ thông tin, tồn giữ nguyên")
        void removeMoreThanStock() {
            OutOfStockException e = assertThrows(OutOfStockException.class, () -> hanoi.remove(TSHIRT, 51));

            assertEquals(TSHIRT, e.getProductId());
            assertEquals(51, e.getRequested());
            assertEquals(50, e.getAvailable());
            assertEquals(50, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("Xuất product chưa có trong kho → OutOfStockException (available = 0)")
        void removeUnknownProduct() {
            OutOfStockException e = assertThrows(OutOfStockException.class, () -> hanoi.remove(JEANS, 1));

            assertEquals(0, e.getAvailable());
            assertEquals(0, hanoi.stockOf(JEANS));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        @DisplayName("quantity <= 0 → IllegalArgumentException")
        void rejectsNonPositiveQuantity(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> hanoi.remove(TSHIRT, quantity));
            assertEquals(50, hanoi.stockOf(TSHIRT));
        }

        @Test
        @DisplayName("productId null → IllegalArgumentException")
        void rejectsNullProductId() {
            assertThrows(IllegalArgumentException.class, () -> hanoi.remove(null, 1));
        }
    }

    @Nested
    @DisplayName("Nhiều thread cùng gọi")
    class Concurrent {

        @RepeatedTest(10)
        @DisplayName("100 thread cùng nhập 10 áo → tồn 1000, không mất lần nào")
        void concurrentAdds() throws Exception {
            List<Throwable> results = Concurrently.run(100, i -> () -> hanoi.add(TSHIRT, 10));

            results.forEach(r -> assertNull(r));
            assertEquals(1000, hanoi.stockOf(TSHIRT));
        }

        @RepeatedTest(10)
        @DisplayName("Tồn 50, 100 thread cùng xuất 1 → đúng 50 lần thành công, tồn 0, không âm")
        void concurrentRemoves() throws Exception {
            hanoi.add(TSHIRT, 50);

            List<Throwable> results = Concurrently.run(100, i -> () -> hanoi.remove(TSHIRT, 1));

            assertEquals(50, results.stream().filter(r -> r == null).count());
            results.stream().filter(r -> r != null).forEach(r -> assertInstanceOf(OutOfStockException.class, r));
            assertEquals(0, hanoi.stockOf(TSHIRT));
        }
    }
}
