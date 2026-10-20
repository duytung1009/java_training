// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// SPEC S03 – nâng cấp 20/10: Product thành abstract → dùng PhysicalProduct (0.5 kg); kỳ vọng S02 giữ nguyên.
// SPEC S05 – nâng cấp 21/10: findById trả Optional (Spec S05 D2).
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryManagerTest {

    private InventoryManager inventory;
    private Product tshirt;
    private Product jeans;

    @BeforeEach
    void setUp() {
        inventory = new InventoryManager();
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.5"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.5"));
        inventory.addProduct(tshirt);
        inventory.addProduct(jeans);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Trả về đúng instance đã add")
        void returnsSameInstanceThatWasAdded() {
            assertSame(tshirt, inventory.findById("P001").orElseThrow());
            assertSame(jeans, inventory.findById("P002").orElseThrow());
        }

        @Test
        @DisplayName("So sánh id theo giá trị (equals), không theo reference (==)")
        void comparesIdByValueNotReference() {
            String idFromUserInput = new String("P001");

            assertSame(tshirt, inventory.findById(idFromUserInput).orElseThrow());
        }

        @Test
        @DisplayName("id không tồn tại → Optional.empty()")
        void returnsNullForUnknownId() {
            assertTrue(inventory.findById("P999").isEmpty());
        }

        @Test
        @DisplayName("id null → Optional.empty()")
        void returnsNullForNullId() {
            assertTrue(inventory.findById(null).isEmpty());
        }

        @Test
        @DisplayName("Kho rỗng → Optional.empty()")
        void returnsNullOnEmptyInventory() {
            assertTrue(new InventoryManager().findById("P001").isEmpty());
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
                added[i] = new PhysicalProduct("P1" + (10 + i), "Sản phẩm số " + (i + 1), new BigDecimal("99000"), 10, new BigDecimal("0.5"));
                bigInventory.addProduct(added[i]);
            }

            for (Product product : added) {
                assertSame(product, bigInventory.findById(product.getId()).orElseThrow());
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
            Product duplicate = new PhysicalProduct(new String("P001"), "Áo thun nam (bản trùng)", new BigDecimal("120000"), 5, new BigDecimal("0.5"));

            assertThrows(IllegalArgumentException.class, () -> inventory.addProduct(duplicate));
            assertSame(tshirt, inventory.findById("P001").orElseThrow());
        }
    }

    // SPEC S02 – sửa 20/10 (Tùng quyết): decreaseStock nhận Order thay cho (productId, quantity);
    // all-or-nothing; cùng product ở nhiều line thì cộng dồn.
    @Nested
    @DisplayName("decreaseStock(Order)")
    class DecreaseStock {

        private Order newOrder(OrderLine... lines) {
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            for (OrderLine line : lines) {
                order.addLine(line);
            }
            return order;
        }

        @Test
        @DisplayName("Trừ kho theo từng line, thấy được qua chính reference product đã add")
        void decreasesStockForEachLine() {
            inventory.decreaseStock(newOrder(new OrderLine(tshirt, 3), new OrderLine(jeans, 2)));

            assertEquals(47, tshirt.getStock());
            assertEquals(18, jeans.getStock());
            assertEquals(47, inventory.findById("P001").orElseThrow().getStock());
        }

        @Test
        @DisplayName("Không ảnh hưởng product không có trong order")
        void doesNotAffectProductsNotInOrder() {
            inventory.decreaseStock(newOrder(new OrderLine(tshirt, 3)));

            assertEquals(20, jeans.getStock());
        }

        @Test
        @DisplayName("Đặt đúng bằng tồn kho → còn 0")
        void allowsDecreasingToZero() {
            inventory.decreaseStock(newOrder(new OrderLine(jeans, 20)));

            assertEquals(0, jeans.getStock());
        }

        @Test
        @DisplayName("Nhiều order liên tiếp trừ cộng dồn đúng")
        void accumulatesSuccessiveOrders() {
            inventory.decreaseStock(newOrder(new OrderLine(tshirt, 3)));
            inventory.decreaseStock(newOrder(new OrderLine(tshirt, 7)));

            assertEquals(40, tshirt.getStock());
        }

        @Test
        @DisplayName("Order rỗng → không thay đổi kho")
        void emptyOrderChangesNothing() {
            inventory.decreaseStock(newOrder());

            assertEquals(50, tshirt.getStock());
            assertEquals(20, jeans.getStock());
        }

        @Test
        @DisplayName("Một line thiếu hàng → IllegalStateException, KHÔNG line nào bị trừ (all-or-nothing)")
        void throwsAndChangesNothingWhenAnyLineIsInsufficient() {
            Order order = newOrder(new OrderLine(tshirt, 3), new OrderLine(jeans, 20));
            inventory.decreaseStock(newOrder(new OrderLine(jeans, 1)));   // jeans còn 19 sau khi order được tạo

            assertThrows(IllegalStateException.class, () -> inventory.decreaseStock(order));
            assertEquals(50, tshirt.getStock());
            assertEquals(19, jeans.getStock());
        }

        @Test
        @DisplayName("Cùng product ở 2 line: cộng dồn, vượt tồn kho → IllegalStateException, kho giữ nguyên")
        void accumulatesQuantityOfSameProductAcrossLines() {
            Order order = newOrder(new OrderLine(tshirt, 30), new OrderLine(tshirt, 30));

            assertThrows(IllegalStateException.class, () -> inventory.decreaseStock(order));
            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("Cùng product ở 2 line, tổng vừa đủ → trừ hết")
        void allowsSameProductAcrossLinesWithinStock() {
            inventory.decreaseStock(newOrder(new OrderLine(tshirt, 20), new OrderLine(tshirt, 30)));

            assertEquals(0, tshirt.getStock());
        }

        @Test
        @DisplayName("Line có product không có trong kho → IllegalArgumentException, kho giữ nguyên")
        void rejectsProductNotInInventory() {
            Product socks = new PhysicalProduct("P003", "Tất cổ ngắn", new BigDecimal("12345.67"), 100, new BigDecimal("0.5"));
            Order order = newOrder(new OrderLine(tshirt, 2), new OrderLine(socks, 1));

            assertThrows(IllegalArgumentException.class, () -> inventory.decreaseStock(order));
            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("order null → IllegalArgumentException")
        void rejectsNullOrder() {
            assertThrows(IllegalArgumentException.class, () -> inventory.decreaseStock(null));
        }
    }
}
