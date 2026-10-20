// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderLineTest {

    private Product tshirt;

    @BeforeEach
    void setUp() {
        tshirt = new Product("P001", "Áo thun nam", new BigDecimal("150000"), 50);
    }

    @Nested
    @DisplayName("Tạo OrderLine")
    class Create {

        @Test
        @DisplayName("Getter trả đúng product và quantity")
        void exposesProductAndQuantity() {
            OrderLine line = new OrderLine(tshirt, 2);

            assertSame(tshirt, line.getProduct());
            assertEquals(2, line.getQuantity());
        }

        @Test
        @DisplayName("quantity bằng đúng tồn kho là hợp lệ")
        void allowsQuantityEqualToStock() {
            OrderLine line = new OrderLine(tshirt, 50);

            assertEquals(50, line.getQuantity());
        }

        @Test
        @DisplayName("quantity lớn hơn tồn kho → IllegalStateException")
        void rejectsQuantityGreaterThanStock() {
            assertThrows(IllegalStateException.class, () -> new OrderLine(tshirt, 51));
        }

        @Test
        @DisplayName("Tạo OrderLine không làm giảm tồn kho (giảm kho là việc của InventoryManager)")
        void doesNotChangeStock() {
            new OrderLine(tshirt, 2);

            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("product null → IllegalArgumentException")
        void rejectsNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> new OrderLine(null, 1));
        }

        @ParameterizedTest(name = "quantity = {0}")
        @ValueSource(ints = {0, -5})
        @DisplayName("quantity <= 0 → IllegalArgumentException")
        void rejectsNonPositiveQuantity(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> new OrderLine(tshirt, quantity));
        }
    }

    @Nested
    @DisplayName("lineTotal")
    class LineTotal {

        @ParameterizedTest(name = "{0} × {1} = {2}")
        @DisplayName("price × quantity, scale 2, làm tròn HALF_UP")
        @CsvSource({
                "150000, 1, 150000.00",
                "150000, 2, 300000.00",
                "12345.67, 3, 37037.01",
                "19999.995, 1, 20000.00",
                "0, 5, 0.00",
        })
        void multipliesPriceByQuantity(String price, int quantity, String expected) {
            Product product = new Product("P010", "Tất cổ ngắn", new BigDecimal(price), 100);

            assertEquals(new BigDecimal(expected), new OrderLine(product, quantity).lineTotal());
        }
    }
}
