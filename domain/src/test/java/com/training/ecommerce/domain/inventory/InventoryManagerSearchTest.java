// SPEC S05 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InventoryManager: findById Optional + search(Predicate) (S05)")
class InventoryManagerSearchTest {

    private InventoryManager inventory;
    private Product tshirt;
    private Product jeans;
    private Product office;
    private Product mouse;

    @BeforeEach
    void setUp() {
        inventory = new InventoryManager();
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"), Category.FASHION);
        office = new DigitalProduct("D001", "Office 2024", new BigDecimal("1000000"), 200, Category.SOFTWARE);
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"), Category.FASHION);
        mouse = new PhysicalProduct("E001", "Chuột không dây", new BigDecimal("250000"), 0, new BigDecimal("0.1"), Category.ELECTRONICS);
        inventory.addProduct(tshirt);
        inventory.addProduct(office);
        inventory.addProduct(jeans);
        inventory.addProduct(mouse);
    }

    @Nested
    @DisplayName("findById → Optional")
    class FindByIdOptional {

        @Test
        @DisplayName("Có → Optional chứa đúng instance, map được sang tên")
        void presentWhenFound() {
            assertTrue(inventory.findById("P001").isPresent());
            assertEquals("Áo thun nam", inventory.findById("P001").map(Product::getName).orElse("?"));
        }

        @Test
        @DisplayName("Không có / null → Optional.empty(), orElse trả giá trị mặc định")
        void emptyWhenMissing() {
            assertTrue(inventory.findById("P999").isEmpty());
            assertTrue(inventory.findById(null).isEmpty());
            assertEquals("không tồn tại", inventory.findById("P999").map(Product::getName).orElse("không tồn tại"));
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("Lọc theo category, giữ thứ tự thêm")
        void filtersByCategoryInInsertionOrder() {
            List<Product> fashion = inventory.search(p -> p.getCategory() == Category.FASHION);

            assertEquals(List.of(tshirt, jeans), fashion);
        }

        @Test
        @DisplayName("Ghép Predicate bằng and / negate")
        void combinesPredicates() {
            Predicate<Product> inStock = p -> p.getStock() > 0;
            Predicate<Product> under500k = p -> p.calculateFinalPrice().compareTo(new BigDecimal("500000")) < 0;

            assertEquals(List.of(tshirt, jeans), inventory.search(inStock.and(under500k)));
            assertEquals(List.of(mouse), inventory.search(inStock.negate()));
        }

        @Test
        @DisplayName("Không khớp → list rỗng; kho rỗng → list rỗng")
        void emptyWhenNothingMatches() {
            assertTrue(inventory.search(p -> false).isEmpty());
            assertTrue(new InventoryManager().search(p -> true).isEmpty());
        }

        @Test
        @DisplayName("Sửa list trả về không ảnh hưởng catalog")
        void resultIsSnapshot() {
            List<Product> all = inventory.search(p -> true);
            try {
                all.clear();
            } catch (UnsupportedOperationException ignored) {
                // list không sửa được cũng hợp lệ
            }

            assertEquals(4, inventory.search(p -> true).size());
        }

        @Test
        @DisplayName("criteria null → IllegalArgumentException")
        void rejectsNullCriteria() {
            assertThrows(IllegalArgumentException.class, () -> inventory.search(null));
        }
    }
}
