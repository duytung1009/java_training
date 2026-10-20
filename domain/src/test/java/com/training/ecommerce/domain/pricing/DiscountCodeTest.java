// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscountCodeTest {

    @Test
    @DisplayName("Getter trả đúng mã và tỉ lệ")
    void exposesValues() {
        DiscountCode code = new DiscountCode("SALE10", new BigDecimal("0.10"));

        assertEquals("SALE10", code.getCode());
        assertEquals(0, new BigDecimal("0.10").compareTo(code.getRate()));
    }

    @Test
    @DisplayName("rate = 1 (giảm 100%) là hợp lệ")
    void acceptsFullDiscount() {
        assertEquals(0, BigDecimal.ONE.compareTo(new DiscountCode("FREE", BigDecimal.ONE).getRate()));
    }

    @ParameterizedTest(name = "rate = {0}")
    @ValueSource(strings = {"0", "-0.1", "1.01"})
    @DisplayName("rate ngoài (0, 1] → IllegalArgumentException")
    void rejectsRateOutOfRange(String rate) {
        assertThrows(IllegalArgumentException.class, () -> new DiscountCode("SALE", new BigDecimal(rate)));
    }

    @Test
    @DisplayName("rate null → IllegalArgumentException")
    void rejectsNullRate() {
        assertThrows(IllegalArgumentException.class, () -> new DiscountCode("SALE", null));
    }

    @ParameterizedTest(name = "code = \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("code null/rỗng/chỉ khoảng trắng → IllegalArgumentException")
    void rejectsBlankCode(String code) {
        assertThrows(IllegalArgumentException.class, () -> new DiscountCode(code, new BigDecimal("0.10")));
    }
}
