// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BundleProductTest {

    private final Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
    private final Product jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
    private final Product office = new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200);
    private final Product claudePro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100);

    @Test
    @DisplayName("Σ giá bán của item × (1 − discountRate): (150.000 + 450.000) × 0.9 = 540.000")
    void discountsSumOfItems() {
        BundleProduct outfit = new BundleProduct("B001", "Combo áo + quần", 10, new BigDecimal("0.10"), tshirt, jeans);

        assertEquals(new BigDecimal("540000.00"), outfit.calculateFinalPrice());
    }

    @Test
    @DisplayName("Item là subtype khác nhau: dùng calculateFinalPrice() của từng loại (polymorphism)")
    void usesEachItemsFinalPrice() {
        BundleProduct workKit = new BundleProduct("B002", "Combo làm việc", 10, new BigDecimal("0.20"), office, claudePro);

        // (1.000.000 + 499.000 × 12) × 0.8 = 6.988.000 × 0.8
        assertEquals(new BigDecimal("5590400.00"), workKit.calculateFinalPrice());
    }

    @Test
    @DisplayName("Bundle chứa bundle khác")
    void supportsNestedBundles() {
        BundleProduct outfit = new BundleProduct("B001", "Combo áo + quần", 10, new BigDecimal("0.10"), tshirt, jeans);
        BundleProduct mega = new BundleProduct("B003", "Siêu combo", 5, new BigDecimal("0.05"), outfit, office);

        // (540.000 + 1.000.000) × 0.95
        assertEquals(new BigDecimal("1463000.00"), mega.calculateFinalPrice());
    }

    @Test
    @DisplayName("discountRate = 0 → đúng tổng giá item; làm tròn HALF_UP")
    void zeroDiscountAndRounding() {
        Product socks = new PhysicalProduct("P003", "Tất cổ ngắn", new BigDecimal("12345.67"), 100, new BigDecimal("0.1"));

        assertEquals(new BigDecimal("12345.67"),
                new BundleProduct("B004", "Tất lẻ", 10, BigDecimal.ZERO, socks).calculateFinalPrice());
        // 12345.67 × 0.85 = 10493.8195
        assertEquals(new BigDecimal("10493.82"),
                new BundleProduct("B005", "Tất giảm 15%", 10, new BigDecimal("0.15"), socks).calculateFinalPrice());
    }

    @Test
    @DisplayName("Không có item → IllegalArgumentException")
    void rejectsEmptyItems() {
        assertThrows(IllegalArgumentException.class,
                () -> new BundleProduct("B006", "Combo rỗng", 10, new BigDecimal("0.10")));
    }

    @Test
    @DisplayName("Item null → IllegalArgumentException")
    void rejectsNullItem() {
        assertThrows(IllegalArgumentException.class,
                () -> new BundleProduct("B007", "Combo lỗi", 10, new BigDecimal("0.10"), tshirt, null));
    }

    @ParameterizedTest(name = "discountRate = {0}")
    @ValueSource(strings = {"-0.01", "1.01"})
    @DisplayName("discountRate ngoài [0, 1] → IllegalArgumentException")
    void rejectsDiscountRateOutOfRange(String rate) {
        assertThrows(IllegalArgumentException.class,
                () -> new BundleProduct("B008", "Combo lỗi", 10, new BigDecimal(rate), tshirt));
    }

    @Test
    @DisplayName("discountRate null → IllegalArgumentException")
    void rejectsNullDiscountRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new BundleProduct("B009", "Combo lỗi", 10, null, tshirt));
    }
}
