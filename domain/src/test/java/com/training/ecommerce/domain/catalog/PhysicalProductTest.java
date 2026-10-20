// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.DiscountCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhysicalProductTest {

    private static final Address HANOI = new Address("17 Duy Tân, Cầu Giấy", "Hà Nội");
    private static final Address HCMC = new Address("12 Nguyễn Huệ, Quận 1", "Hồ Chí Minh");

    private PhysicalProduct tshirt;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
    }

    @Nested
    @DisplayName("Tạo PhysicalProduct")
    class Create {

        @Test
        @DisplayName("Getter trả đúng giá trị, kể cả cân nặng")
        void exposesGivenValues() {
            assertEquals("P001", tshirt.getId());
            assertEquals("Áo thun nam", tshirt.getName());
            assertEquals(0, new BigDecimal("150000").compareTo(tshirt.getBasePrice()));
            assertEquals(50, tshirt.getStock());
            assertEquals(0, new BigDecimal("0.3").compareTo(tshirt.getWeightKg()));
        }

        @ParameterizedTest(name = "weightKg = {0}")
        @ValueSource(strings = {"0", "-0.5"})
        @DisplayName("Cân nặng <= 0 → IllegalArgumentException")
        void rejectsNonPositiveWeight(String weight) {
            assertThrows(IllegalArgumentException.class,
                    () -> new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal(weight)));
        }

        @Test
        @DisplayName("Cân nặng null → IllegalArgumentException")
        void rejectsNullWeight() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, null));
        }

        @Test
        @DisplayName("Vẫn giữ quy tắc của Product: giá âm → IllegalArgumentException")
        void keepsProductRules() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("-1"), 50, new BigDecimal("0.3")));
        }
    }

    @Nested
    @DisplayName("calculateFinalPrice")
    class FinalPrice {

        @Test
        @DisplayName("Bằng giá gốc, scale 2 (chưa gồm giảm giá và phí ship)")
        void equalsBasePrice() {
            assertEquals(new BigDecimal("150000.00"), tshirt.calculateFinalPrice());
        }
    }

    @Nested
    @DisplayName("applyDiscount")
    class ApplyDiscount {

        @ParameterizedTest(name = "giảm {0} → {1}")
        @DisplayName("Giá sau giảm = final × (1 − rate), scale 2, HALF_UP")
        @CsvSource({
                "0.10, 135000.00",
                "0.25, 112500.00",
                "0.333, 100050.00",
                "1, 0.00",
        })
        void appliesRate(String rate, String expected) {
            assertEquals(new BigDecimal(expected), tshirt.applyDiscount(new DiscountCode("SALE", new BigDecimal(rate))));
        }

        @Test
        @DisplayName("Không làm thay đổi giá bán của product")
        void doesNotChangeFinalPrice() {
            tshirt.applyDiscount(new DiscountCode("SALE10", new BigDecimal("0.10")));

            assertEquals(new BigDecimal("150000.00"), tshirt.calculateFinalPrice());
        }

        @Test
        @DisplayName("code null → IllegalArgumentException")
        void rejectsNullCode() {
            assertThrows(IllegalArgumentException.class, () -> tshirt.applyDiscount(null));
        }
    }

    @Nested
    @DisplayName("calculateShipping")
    class CalculateShipping {

        @Test
        @DisplayName("Hà Nội: 20.000 + 5.000 × 0.3 kg = 21.500")
        void innerCityFee() {
            assertEquals(new BigDecimal("21500.00"), tshirt.calculateShipping(HANOI));
        }

        @Test
        @DisplayName("Tỉnh/thành khác: 35.000 + 5.000 × 0.3 kg = 36.500")
        void otherCityFee() {
            assertEquals(new BigDecimal("36500.00"), tshirt.calculateShipping(HCMC));
        }

        @ParameterizedTest(name = "{0} kg ở Hà Nội → {1}")
        @DisplayName("Phí theo cân nặng, scale 2, HALF_UP")
        @CsvSource({
                "2.5, 32500.00",
                "0.0001, 20000.50",
                "0.3333, 21666.50",
        })
        void feeDependsOnWeight(String weight, String expected) {
            PhysicalProduct rice = new PhysicalProduct("P005", "Gạo ST25", new BigDecimal("180000"), 30, new BigDecimal(weight));

            assertEquals(new BigDecimal(expected), rice.calculateShipping(HANOI));
        }

        @Test
        @DisplayName("address null → IllegalArgumentException")
        void rejectsNullAddress() {
            assertThrows(IllegalArgumentException.class, () -> tshirt.calculateShipping(null));
        }
    }
}
