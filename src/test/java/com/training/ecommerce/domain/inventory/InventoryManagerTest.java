// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryManagerTest {

    private InventoryManager inventory;
    private Product tshirt;
    private Product jeans;

    @BeforeEach
    void setUp() {
        inventory = new InventoryManager();
        tshirt = new Product("P001", "Áo thun nam", new BigDecimal("150000"), 50);
        jeans = new Product("P002", "Quần jean", new BigDecimal("450000"), 20);
        inventory.addProduct(tshirt);
        inventory.addProduct(jeans);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Trả về đúng instance đã add")
        void returnsSameInstanceThatWasAdded() {
            assertSame(tshirt, inventory.findById("P001"));
            assertSame(jeans, inventory.findById("P002"));
        }

        @Test
        @DisplayName("So sánh id theo giá trị (equals), không theo reference (==)")
        void comparesIdByValueNotReference() {
            String idFromUserInput = new String("P001");

            assertSame(tshirt, inventory.findById(idFromUserInput));
        }

        @Test
        @DisplayName("id không tồn tại → null")
        void returnsNullForUnknownId() {
            assertNull(inventory.findById("P999"));
        }

        @Test
        @DisplayName("id null → null")
        void returnsNullForNullId() {
            assertNull(inventory.findById(null));
        }

        @Test
        @DisplayName("Kho rỗng → null")
        void returnsNullOnEmptyInventory() {
            assertNull(new InventoryManager().findById("P001"));
        }
    }

    @Nested
    @DisplayName("addProduct")
    class AddProduct {

        @Test
        @DisplayName("Thêm 15 product (vượt kích thước mảng ban đầu) vẫn tìm thấy đủ")
        void growsBeyondInitialCapacity() {
            InventoryManager bigInventory = new InventoryManager();
            Product[] added = new Product[15];
            for (int i = 0; i < added.length; i++) {
                added[i] = new Product("P1" + (10 + i), "Sản phẩm số " + (i + 1), new BigDecimal("99000"), 10);
                bigInventory.addProduct(added[i]);
            }

            for (Product product : added) {
                assertSame(product, bigInventory.findById(product.getId()));
            }
        }

        @Test
        @DisplayName("product null → IllegalArgumentException")
        void rejectsNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> inventory.addProduct(null));
        }

        @Test
        @DisplayName("Trùng id → IllegalArgumentException, product cũ giữ nguyên")
        void rejectsDuplicateId() {
            Product duplicate = new Product(new String("P001"), "Áo thun nam (bản trùng)", new BigDecimal("120000"), 5);

            assertThrows(IllegalArgumentException.class, () -> inventory.addProduct(duplicate));
            assertSame(tshirt, inventory.findById("P001"));
        }
    }

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStock {

        @Test
        @DisplayName("Giảm tồn kho, thấy được qua chính reference product đã add")
        void decreasesStockOfStoredProduct() {
            inventory.decreaseStock("P001", 3);

            assertEquals(47, tshirt.getStock());
            assertEquals(47, inventory.findById("P001").getStock());
        }

        @Test
        @DisplayName("Không ảnh hưởng product khác")
        void doesNotAffectOtherProducts() {
            inventory.decreaseStock("P001", 3);

            assertEquals(20, jeans.getStock());
        }

        @Test
        @DisplayName("Giảm đúng bằng tồn kho → còn 0")
        void allowsDecreasingToZero() {
            inventory.decreaseStock("P002", 20);

            assertEquals(0, jeans.getStock());
        }

        @Test
        @DisplayName("Giảm nhiều lần liên tiếp cộng dồn đúng")
        void accumulatesSuccessiveDecreases() {
            inventory.decreaseStock("P001", 3);
            inventory.decreaseStock("P001", 7);

            assertEquals(40, tshirt.getStock());
        }

        @Test
        @DisplayName("Thiếu hàng → IllegalStateException, tồn kho giữ nguyên")
        void throwsWhenInsufficientStock() {
            assertThrows(IllegalStateException.class, () -> inventory.decreaseStock("P002", 21));
            assertEquals(20, jeans.getStock());
        }

        @Test
        @DisplayName("Thiếu hàng sau vài lần giảm → IllegalStateException, giữ số đã giảm trước đó")
        void throwsWhenInsufficientAfterPreviousDecreases() {
            inventory.decreaseStock("P001", 10);

            assertThrows(IllegalStateException.class, () -> inventory.decreaseStock("P001", 41));
            assertEquals(40, tshirt.getStock());
        }

        @ParameterizedTest(name = "quantity = {0}")
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("quantity <= 0 → IllegalArgumentException, tồn kho giữ nguyên")
        void rejectsNonPositiveQuantity(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> inventory.decreaseStock("P001", quantity));
            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("id không tồn tại → IllegalArgumentException")
        void rejectsUnknownId() {
            assertThrows(IllegalArgumentException.class, () -> inventory.decreaseStock("P999", 1));
        }

        @Test
        @DisplayName("id null → IllegalArgumentException")
        void rejectsNullId() {
            assertThrows(IllegalArgumentException.class, () -> inventory.decreaseStock(null, 1));
        }
    }
}
