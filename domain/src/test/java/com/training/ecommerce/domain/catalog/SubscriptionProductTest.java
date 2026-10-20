// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionProductTest {

    @ParameterizedTest(name = "{0} × {1} tháng = {2}")
    @DisplayName("calculateFinalPrice = giá tháng × số tháng, scale 2, HALF_UP")
    @CsvSource({
            "499000, 1, 499000.00",
            "499000, 12, 5988000.00",
            "99999.995, 2, 199999.99",
    })
    void multipliesMonthlyPriceByMonths(String monthlyPrice, int months, String expected) {
        SubscriptionProduct plan = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal(monthlyPrice), months, 100);

        assertEquals(new BigDecimal(expected), plan.calculateFinalPrice());
    }

    @Test
    @DisplayName("getBasePrice là giá theo tháng, getMonths là số tháng")
    void exposesMonthlyPriceAndMonths() {
        SubscriptionProduct plan = new SubscriptionProduct("S002", "Gói Netflix Premium", new BigDecimal("260000"), 6, 100);

        assertEquals(0, new BigDecimal("260000").compareTo(plan.getBasePrice()));
        assertEquals(6, plan.getMonths());
    }

    @ParameterizedTest(name = "months = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("months < 1 → IllegalArgumentException")
    void rejectsMonthsBelowOne(int months) {
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), months, 100));
    }

    @Test
    @DisplayName("Vẫn giữ quy tắc của Product: giá tháng âm → IllegalArgumentException")
    void keepsProductRules() {
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("-1"), 12, 100));
    }
}
