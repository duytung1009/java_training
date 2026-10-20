// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.pricing.DiscountCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigitalProductTest {

    private final DigitalProduct office = new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200);

    @Test
    @DisplayName("calculateFinalPrice bằng giá gốc, scale 2")
    void finalPriceEqualsBasePrice() {
        assertEquals(new BigDecimal("1000000.00"), office.calculateFinalPrice());
    }

    @ParameterizedTest(name = "giảm {0} → {1}")
    @DisplayName("applyDiscount: final × (1 − rate)")
    @CsvSource({
            "0.25, 750000.00",
            "0.5, 500000.00",
            "1, 0.00",
    })
    void appliesRate(String rate, String expected) {
        assertEquals(new BigDecimal(expected), office.applyDiscount(new DiscountCode("BACK2SCHOOL", new BigDecimal(rate))));
    }

    @Test
    @DisplayName("applyDiscount(null) → IllegalArgumentException")
    void rejectsNullCode() {
        assertThrows(IllegalArgumentException.class, () -> office.applyDiscount(null));
    }

    @Test
    @DisplayName("Vẫn giữ quy tắc của Product: name rỗng → IllegalArgumentException")
    void keepsProductRules() {
        assertThrows(IllegalArgumentException.class,
                () -> new DigitalProduct("D001", "  ", new BigDecimal("1000000"), 200));
    }
}
