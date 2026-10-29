// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// SPEC S03 – nâng cấp 20/10: Product thành abstract (test qua subclass ẩn danh), getPrice → getBasePrice.
package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    // SPEC S03 – Product là abstract: test contract chung qua subclass tối thiểu.
    private static Product product(String id, String name, BigDecimal basePrice, int stock) {
        return new Product(id, name, basePrice, stock) {
            @Override
            public BigDecimal calculateFinalPrice() {
                return getBasePrice();
            }
        };
    }

    private static Product product(String id, String name, BigDecimal basePrice) {
        return new Product(id, name, basePrice) {
            @Override
            public BigDecimal calculateFinalPrice() {
                return getBasePrice();
            }
        };
    }

    private static final BigDecimal TSHIRT_PRICE = new BigDecimal("150000");

    @Nested
    @DisplayName("Constructor đầy đủ (id, name, price, stock)")
    class FullConstructor {

        @Test
        @DisplayName("Getter trả đúng giá trị truyền vào")
        void exposesGivenValues() {
            Product tshirt = product("P001", "Áo thun nam", TSHIRT_PRICE, 50);

            assertEquals("P001", tshirt.getId());
            assertEquals("Áo thun nam", tshirt.getName());
            assertEquals(0, TSHIRT_PRICE.compareTo(tshirt.getBasePrice()), "price phải bằng 150000");
            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("Giá 0 là hợp lệ (hàng tặng)")
        void acceptsZeroPrice() {
            Product gift = product("P099", "Túi vải quà tặng", BigDecimal.ZERO, 100);

            assertEquals(0, BigDecimal.ZERO.compareTo(gift.getBasePrice()));
        }

        @Test
        @DisplayName("Tồn kho 0 là hợp lệ")
        void acceptsZeroStock() {
            Product soldOut = product("P002", "Quần jean", new BigDecimal("450000"), 0);

            assertEquals(0, soldOut.getStock());
        }

        @Test
        @DisplayName("Giá âm → IllegalArgumentException")
        void rejectsNegativePrice() {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", "Áo thun nam", new BigDecimal("-1"), 50));
        }

        @Test
        @DisplayName("Giá null → IllegalArgumentException")
        void rejectsNullPrice() {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", "Áo thun nam", null, 50));
        }

        @Test
        @DisplayName("Tồn kho âm → IllegalArgumentException")
        void rejectsNegativeStock() {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", "Áo thun nam", TSHIRT_PRICE, -1));
        }

        @ParameterizedTest(name = "id = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("id null/rỗng/chỉ khoảng trắng → IllegalArgumentException")
        void rejectsBlankId(String id) {
            assertThrows(IllegalArgumentException.class,
                    () -> product(id, "Áo thun nam", TSHIRT_PRICE, 50));
        }

        @ParameterizedTest(name = "name = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("name null/rỗng/chỉ khoảng trắng → IllegalArgumentException")
        void rejectsBlankName(String name) {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", name, TSHIRT_PRICE, 50));
        }
    }

    @Nested
    @DisplayName("Constructor không có stock (id, name, price)")
    class WithoutStockConstructor {

        @Test
        @DisplayName("Tồn kho mặc định là 0, các field khác đúng giá trị")
        void defaultsStockToZero() {
            Product tshirt = product("P001", "Áo thun nam", TSHIRT_PRICE);

            assertEquals("P001", tshirt.getId());
            assertEquals("Áo thun nam", tshirt.getName());
            assertEquals(0, TSHIRT_PRICE.compareTo(tshirt.getBasePrice()));
            assertEquals(0, tshirt.getStock());
        }

        @Test
        @DisplayName("Cùng quy tắc kiểm tra: giá âm → IllegalArgumentException")
        void rejectsNegativePrice() {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", "Áo thun nam", new BigDecimal("-150000")));
        }

        @Test
        @DisplayName("Cùng quy tắc kiểm tra: name rỗng → IllegalArgumentException")
        void rejectsBlankName() {
            assertThrows(IllegalArgumentException.class,
                    () -> product("P001", " ", TSHIRT_PRICE));
        }
    }

    // SPEC S07-extra – thêm 30/10 (warehouse-hub): nhập hàng kho → hub cộng stock qua increaseStock.
    @Nested
    @DisplayName("increaseStock")
    class IncreaseStock {

        @Test
        @DisplayName("Stock 5, cộng 3 → 8")
        void increaseAddsStock() {
            Product tshirt = product("P001", "Áo thun nam", TSHIRT_PRICE, 5);

            tshirt.increaseStock(3);

            assertEquals(8, tshirt.getStock());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("quantity <= 0 → IllegalArgumentException, stock giữ nguyên")
        void increaseRejectsNonPositive(int quantity) {
            Product tshirt = product("P001", "Áo thun nam", TSHIRT_PRICE, 5);

            assertThrows(IllegalArgumentException.class, () -> tshirt.increaseStock(quantity));
            assertEquals(5, tshirt.getStock());
        }
    }
}
